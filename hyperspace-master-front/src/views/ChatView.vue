<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch,computed,onActivated } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/userStore'
import { getUserInfo, getFullAvatarUrl } from '../utils/user'
import apiClient, { API_ENDPOINTS, uploadFileToOSSDirectly } from '../services/api' // 添加 uploadFileToOSSDirectly
import  globalWebSocketManager  from '../services/globalWebSocketManager'
import type {CSSProperties} from "vue";

// 定义消息对象接口
interface Message {
  messageId: string;
  type: string;
  text: string;
  time: string;
  isSent: boolean;
  showDate: boolean;
  createdAt: string;
  clientTimestamp: number;
  // 引用消息相关字段
  quoteMessageId?: string;
  quoteMessageContent?: string;
  quoteMessageSenderName?: string;
  // 图片消息相关字段
  imageUrls?: string;
  // 文件消息相关字段
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
  [key: string]: any; // 允许其他属性
}

// 定义发送消息对象接口
interface SendMessage {
  type: string;
  toTargetId: string;
  toTargetType: string;
  textContent?: string;
  imageUrls?: string;
  fileUrl?: string;
  fileName?: string;
  fileSize?: number;
  clientTimestamp: number;
  quoteMessageId?: string;
  [key: string]: any; // 允许其他属性
}
// 获取路由实例
const route = useRoute()

// 定义响应式数据
const conversations = ref<any[]>([])
const activeConversation = ref<any>(null)
const messages = ref<Message[]>([])
const newMessage = ref('')
const messagesContainer = ref<HTMLElement | null>(null)
const unreadCounts = ref<Map<string, number>>(new Map()) // 添加未读消息计数
const unreadRefreshInterval = ref<number | null>(null) // 定时刷新未读消息的interval ID
const selectedImage = ref<File | null>(null) // 选中的图片文件

// 引用消息相关
const quotedMessage = ref<Message | null>(null);

// 设置引用消息的函数
const setQuotedMessage = (message: Message) => {
  quotedMessage.value = message;
  console.log('设置引用消息:', message);
  
  // 设置焦点到输入框
  nextTick(() => {
    const inputElement = document.querySelector('.message-input textarea');
    if (inputElement) {
      (inputElement as HTMLTextAreaElement).focus();
    }
  });
};

// 取消引用消息
const cancelQuotedMessage = () => {
  quotedMessage.value = null;
};

// 获取用户store
const userStore = useUserStore()

// 背景设置
const backgroundSettings = ref({
  backgroundImage: '',
  backgroundOpacity: 100
})

// 添加控制附件工具栏展开/收起的状态
const isAttachmentToolbarOpen = ref(false)

// 切换附件工具栏的展开/收起状态
const toggleAttachmentToolbar = () => {
  isAttachmentToolbarOpen.value = !isAttachmentToolbarOpen.value;
};

// 计算背景样式
const backgroundStyle = computed((): CSSProperties => {
  if (backgroundSettings.value.backgroundImage) {
    return {
      backgroundImage: `url(${backgroundSettings.value.backgroundImage})`,
      backgroundSize: 'cover',
      backgroundPosition: 'center',
      backgroundRepeat: 'no-repeat',
      position: 'absolute',
      top: '0px',
      left: '0px',
      right: '0px',
      bottom: '0px',
      zIndex: 0
    }
  }
  // 如果没有设置聊天背景，则使用默认背景
  return {
    backgroundImage: 'none',
    backgroundColor: 'transparent',
    position: 'absolute',
    top: '0px',
    left: '0px',
    right: '0px',
    bottom: '0px',
    zIndex: 0
  } as CSSProperties
})


// 计算覆盖层样式
const overlayStyle = computed((): CSSProperties => {
  if (backgroundSettings.value.backgroundImage) {
    // 计算透明度 (0-1之间)
    const opacity = 1 - (backgroundSettings.value.backgroundOpacity / 100);
    return {
      backgroundColor: userStore.getDarkMode ? '#1a1a1a' : 'white',
      opacity: opacity,
      position: 'absolute',
      top: '0px',
      left: '0px',
      right: '0px',
      bottom: '0px',
      zIndex: 1
    } as CSSProperties
  }
  // 如果没有背景图片，返回透明覆盖层
  return {
    backgroundColor: 'transparent',
    opacity: 0,
    position: 'absolute',
    top: '0px',
    left: '0px',
    right: '0px',
    bottom: '0px',
    zIndex: 1
  } as CSSProperties
})

// 格式化日期显示
const formatDate = (date: string) => {
  const messageDate = new Date(date);
  const today = new Date();

  // 检查是否是今天
  if (messageDate.toDateString() === today.toDateString()) {
    return '今天';
  }

  // 检查是否是昨天
  const yesterday = new Date(today);
  yesterday.setDate(yesterday.getDate() - 1);
  if (messageDate.toDateString() === yesterday.toDateString()) {
    return '昨天';
  }

  // 其他日期格式化为年月日
  return `${messageDate.getFullYear()}年${messageDate.getMonth() + 1}月${messageDate.getDate()}日`;
}

// 监听暗色模式变化并应用到DOM
watch(() => userStore.getDarkMode, (newDarkMode) => {
  document.body.classList.toggle('dark-mode', newDarkMode);
  
  // 在亮色模式下清除body的背景图片
  if (!newDarkMode) {
    document.body.style.backgroundImage = 'none';
  }
  
  // 强制更新背景样式以确保覆盖层颜色正确
  // 通过创建一个新的对象引用来触发computed属性重新计算
  backgroundSettings.value = { ...backgroundSettings.value };
})

// 更新背景设置
const updateBackgroundSettings = () => {
  const savedBackgroundImage = localStorage.getItem('userBackgroundImage')
  const savedBackgroundOpacity = localStorage.getItem('userBackgroundOpacity')

  if (savedBackgroundImage) {
    backgroundSettings.value.backgroundImage = savedBackgroundImage
  } else {
    backgroundSettings.value.backgroundImage = ''
  }

  if (savedBackgroundOpacity) {
    backgroundSettings.value.backgroundOpacity = parseInt(savedBackgroundOpacity, 10)
  } else {
    backgroundSettings.value.backgroundOpacity = 100
  }
  
  // 更新store中的背景状态
  userStore.setHasBackground(!!savedBackgroundImage)
}

// 添加处理暗色模式变化事件
const handleDarkModeChanged = (e: CustomEvent) => {
  const detail = e.detail as { darkMode: boolean };
  userStore.setDarkMode(detail.darkMode);
};

// 处理背景透明度变化事件
const handleBackgroundOpacityChanged = (e: CustomEvent) => {
  const detail = e.detail as { opacity: number };
  backgroundSettings.value.backgroundOpacity = detail.opacity;
};

// 添加处理背景图片变化事件
const handleBackgroundImageChanged = (e: CustomEvent) => {
  const detail = e.detail as { backgroundImage: string };
  backgroundSettings.value.backgroundImage = detail.backgroundImage;
  // 更新store中的背景状态
  userStore.setHasBackground(!!detail.backgroundImage);
};

// 处理存储变化事件
const handleStorageChanged = (e: StorageEvent) => {
  if (e.key === 'userBackgroundImage' || e.key === 'userBackgroundOpacity' || e.key === 'userDarkMode') {
    updateBackgroundSettings()
  }
};

// 处理用户状态变更
const handleUserStatusChange = (data: any) => {
  console.log('开始处理用户状态变更:', data);

  // 检查data是否有效
  if (!data || !data.userId) {
    console.log('无效的用户状态数据');
    return;
  }

  // 使用Vue的响应式系统更新状态
  // 直接修改数组元素以确保响应式更新
  for (let i = 0; i < conversations.value.length; i++) {
    if (conversations.value[i].id === data.userId) {
      console.log(`找到用户 ${data.userId}，原状态: ${conversations.value[i].status}`);
      // 直接修改数组元素的属性
      conversations.value[i].status = data.status ? '在线' : '离线';
      console.log(`用户状态已更新为: ${conversations.value[i].status}`);

      // 如果当前活跃会话是该用户，也更新活跃会话的状态
      if (activeConversation.value && activeConversation.value.id === data.userId) {
        console.log(`更新当前会话用户 ${data.userId} 状态`);
        activeConversation.value.status = data.status ? '在线' : '离线';
        console.log('当前会话已更新');
      }

      // 强制触发Vue的响应式更新
      conversations.value = [...conversations.value];
      break;
    }
  }
};

// 处理实时消息
const handleRealTimeMessage = (message: any) => {
  console.log('收到实时消息:', message);
  console.log('引用消息ID:', message.quoteMessageId);
  console.log('引用消息内容:', message.quoteMessageContent);
  console.log('引用消息发送者:', message.quoteMessageSenderName);

  // 检查消息是否已存在（避免重复显示）
  const exists = messages.value.some(msg => msg.messageId === message.messageId);
  if (exists) {
    console.log('消息已存在，跳过:', message.messageId);
    return;
  }

  // 确定对话伙伴ID（不是当前用户ID的另一个用户）
  const partnerId = message.fromUserId === getUserInfo()?.userId ? message.toTargetId : message.fromUserId;

  // 处理当前活跃会话的消息
  if (activeConversation.value && activeConversation.value.id === partnerId) {
    // 添加消息到列表
    const newMsg: any = {
      messageId: message.messageId,
      type: message.type,
      text: message.type === 'image' ? '[图片]' : message.textContent,
      time: message.serverTimestamp ? new Date(message.serverTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }): new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isSent: message.fromUserId === getUserInfo()?.userId,
      showDate: message.showDate || false, // 如果后端提供了showDate属性则使用，否则默认为false
      createdAt: message.serverTimestamp ? new Date(message.serverTimestamp).toISOString() : new Date().toISOString(),
      // 引用消息相关字段
      quoteMessageId: message.quoteMessageId || null,
      quoteMessageContent: message.quoteMessageContent || null,
      quoteMessageSenderName: message.quoteMessageSenderName || null,
      quoteMessageType: message.quoteMessageType || null,
      quoteMessageImageUrl: message.quoteMessageImageUrl || null,
      quoteMessageFileUrl: message.quoteMessageFileUrl || null,
      quoteMessageFileName: message.quoteMessageFileName || null,
      quoteMessageFileSize: message.quoteMessageFileSize || null
    };
    
    console.log('处理后的消息对象:', newMsg);

    // 根据消息类型添加特定字段
    if (message.type === 'image') {
      newMsg.imageUrls = message.imageUrls;
    } else if (message.type === 'file') {
      newMsg.fileUrl = message.fileUrl;
      newMsg.fileName = message.fileName;
      newMsg.fileSize = message.fileSize;
    }

    // 检查是否需要显示日期（如果这是当天第一条消息）
    if (messages.value.length === 0) {
      newMsg.showDate = true;
    } else {
      // 获取最后一条消息的日期
      const lastMessage = messages.value[messages.value.length - 1];
      if (lastMessage && lastMessage.createdAt) {
        const lastMessageDate = new Date(lastMessage.createdAt);
        const newMessageDate = new Date(newMsg.createdAt);

        // 如果日期不同，则显示日期
        if (lastMessageDate.toDateString() !== newMessageDate.toDateString()) {
          newMsg.showDate = true;
        }
      }
    }

    messages.value.push(newMsg);

    // 滚动到底部
    scrollToBottom();

    // 如果消息来自其他用户，标记为已读
    if (message.fromUserId !== getUserInfo()?.userId) {
      markMessagesAsRead(message.fromUserId);
    }
  }

  // 更新会话列表中的最后消息（无论是否是当前会话）
  conversations.value = conversations.value.map(conversation => {
    if (conversation.id === partnerId) {
      let lastMessage = message.textContent;
      if (message.type === 'image') {
        lastMessage = '[图片]';
      } else if (message.type === 'file') {
        lastMessage = `[文件] ${message.fileName || ''}`;
      }
      
      return {
        ...conversation,
        lastMessage: lastMessage,
        time: message.serverTimestamp ? new Date(message.serverTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
      };
    }
    return conversation;
  });

  // 如果不是当前会话的消息，增加未读计数
  if (!activeConversation.value || activeConversation.value.id !== partnerId) {
    const currentCount = unreadCounts.value.get(partnerId) || 0;
    unreadCounts.value.set(partnerId, currentCount + 1);
  }
};

// 选择会话
const selectConversation = async (conversation: any) => {
  activeConversation.value = conversation
  // 清除未读计数
  unreadCounts.value.delete(conversation.id);
  // 加载消息
  await loadMessages(conversation.id)

  // 标记消息为已读
  if (conversation.id) {
    await markMessagesAsRead(conversation.id);
  }

  // 确保在DOM更新后滚动到底部
  setTimeout(() => {
    scrollToBottom()
  }, 10)
}

// 加载消息
const loadMessages = async (conversationId: string) => {
  try {
    const response = await apiClient.get(API_ENDPOINTS.MESSAGE_HISTORY, {
      params: { friendId: conversationId }
    })

    console.log('加载消息历史:', response);

    // 确保响应数据存在且为数组
    if (response && Array.isArray(response.data)) {
      messages.value = response.data.map((msg: any) => {
           const baseMessage: any = {
          messageId: msg.messageId,
          type: msg.type || 'text',
          text: msg.textContent,
          time: msg.serverTimestamp ? new Date(msg.serverTimestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }): '',
          isSent: msg.fromUserId !== conversationId,
          showDate: msg.showDate || false,
          createdAt: msg.serverTimestamp ? new Date(msg.serverTimestamp).toISOString() : '',
          // 引用消息相关字段
          quoteMessageId: msg.quoteMessageId || null,
          quoteMessageContent: msg.quoteMessageContent || null,
          quoteMessageSenderName: msg.quoteMessageSenderName || null,
          quoteMessageType: msg.quoteMessageType || null,
          quoteMessageImageUrl: msg.quoteMessageImageUrl || null,
          quoteMessageFileUrl: msg.quoteMessageFileUrl || null,
          quoteMessageFileName: msg.quoteMessageFileName || null,
          quoteMessageFileSize: msg.quoteMessageFileSize || null
        };

        // 根据消息类型添加特定字段
        if (msg.type === 'image') {
          baseMessage.imageUrls = msg.imageUrls;
        } else if (msg.type === 'file') {
          baseMessage.fileUrl = msg.fileUrl;
          baseMessage.fileName = msg.fileName;
          baseMessage.fileSize = msg.fileSize;
        }

        return baseMessage;
      });
    } else {
      // 如果没有消息或响应格式不正确，初始化为空数组
      messages.value = []
    }

    // 滚动到底部
    setTimeout(() => {
      scrollToBottom()
    }, 0)
  } catch (error: any) {
    // 检查是否是认证错误
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      ElMessage.error('登录已过期，请重新登录')
      return
    }

    ElMessage.error('加载消息失败: ' + (error.message || '未知错误'))
    // 清空消息列表而不是显示固定消息
    messages.value = []
  }
}

// 发送消息
const sendMessage = async () => {
  if (!newMessage.value.trim() || !activeConversation.value) {
    return;
  }

  try {
    // 如果有选中的图片，则发送图片消息
    if (selectedImage.value) {
      await sendImageMessage();
      return;
    }

    // 构造本地消息对象（用于立即显示）
    const localMessage: Message = {
      messageId: 'temp_' + Date.now().toString(), // 临时ID
      type: 'text',
      text: newMessage.value,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }), // 修复时间格式
      isSent: true,
      showDate: false,
      createdAt: new Date().toISOString(),
      clientTimestamp: Date.now() // 添加客户端时间戳
    };

    // 如果有引用消息，添加引用相关信息
    if (quotedMessage.value) {
      localMessage.quoteMessageId = quotedMessage.value.messageId;
      localMessage.quoteMessageContent = quotedMessage.value.text;
      localMessage.quoteMessageSenderName = quotedMessage.value.isSent ? 
        getUserInfo()?.userName : activeConversation.value.name;
      
      // 根据被引用消息类型设置quoteMessageType
      if (quotedMessage.value.type === 'image') {
        localMessage.quoteMessageType = 'image';
        localMessage.quoteMessageImageUrl = getImageUrl(quotedMessage.value.imageUrls || '');
      } else if (quotedMessage.value.type === 'file') {
        localMessage.quoteMessageType = 'file';
        localMessage.quoteMessageFileName = quotedMessage.value.fileName;
        localMessage.quoteMessageFileSize = quotedMessage.value.fileSize;
        localMessage.quoteMessageFileUrl = quotedMessage.value.fileUrl;
      } else {
        localMessage.quoteMessageType = 'text';
      }
    }

    // 立即添加到消息列表（优化用户体验）
    messages.value.push(localMessage);

    // 滚动到底部
    scrollToBottom();

    // 通过WebSocket发送消息而不是HTTP请求
    if (globalWebSocketManager.isConnected()) {
      const messageToSend: SendMessage = {
        type: 'text',
        toTargetId: activeConversation.value.id,
        toTargetType: 'user',
        textContent: newMessage.value,
        clientTimestamp: Date.now()
      };

      // 如果有引用消息，添加引用相关信息
      if (quotedMessage.value) {
        messageToSend.quoteMessageId = quotedMessage.value.messageId;
      }

      globalWebSocketManager.sendMessage(messageToSend);
    } else {
      ElMessage.error('WebSocket未连接，无法发送消息');
      // 如果发送失败，从消息列表中移除本地消息
      const index = messages.value.findIndex((msg: Message) => msg.messageId === localMessage.messageId);
      if (index !== -1) {
        messages.value.splice(index, 1);
      }
    }
    
    // 清空输入框和引用消息
    newMessage.value = '';
    quotedMessage.value = null;
  } catch (error: any) {
    ElMessage.error('发送消息时出错: ' + (error.message || '未知错误'));
  }
}

// 选择图片
const selectImage = () => {
  // 创建一个隐藏的文件输入框
  const fileInput = document.createElement('input');
  fileInput.type = 'file';
  fileInput.accept = 'image/*';
  fileInput.onchange = (e) => {
    const target = e.target as HTMLInputElement;
    if (target.files && target.files[0]) {
      selectedImage.value = target.files[0];
      // 自动发送图片消息
      sendImageMessage();
    }
  };
  fileInput.click();
};

// 选择文件
const selectFile = () => {
  // 创建一个隐藏的文件输入框
  const fileInput = document.createElement('input');
  fileInput.type = 'file';
  fileInput.onchange = (e) => {
    const target = e.target as HTMLInputElement;
    if (target.files && target.files[0]) {
      const selectedFile = target.files[0];
      // 发送文件消息
      sendFileMessage(selectedFile);
    }
  };
  fileInput.click();
};


// 发送图片消息时显示加载提示
const sendImageMessage = async () => {
  if (!selectedImage.value || !activeConversation.value) {
    return;
  }

  try {
    // 显示上传提示
    const loadingMessage = ElMessage({
      message: '正在上传图片...',
      type: 'info',
      duration: 0 // 不自动关闭
    });

    let imageUrl;
    
    // 检查是否启用直传OSS功能
    if (import.meta.env.VITE_OSS_DIRECT_UPLOAD === 'true') {
      // 使用直传OSS
      imageUrl = await uploadFileToOSSDirectly(selectedImage.value, 'message');
    } else {
      // 使用原有的后端上传方式（保留作为备选方案）
      // 创建FormData对象用于上传图片
      const formData = new FormData();
      formData.append('file', selectedImage.value);
      formData.append('fileType', 'message');

      // 上传图片到服务器
      const response = await apiClient.post<string>(API_ENDPOINTS.FILE_UPLOAD, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });

      // 注意：由于apiClient的响应拦截器，response已经是response.data（ResVO对象）
      // 所以我们可以直接访问code和msg属性
      if (response && response.code === 200) {
        imageUrl = response.data;
      } else {
        throw new Error(response?.msg || '上传失败');
      }
    }

    // 关闭加载提示
    loadingMessage.close();

    // 构造本地图片消息对象（用于立即显示）
    const localMessage: Message = {
      messageId: 'temp_' + Date.now().toString(), // 临时ID
      type: 'image',
      text: '[图片]',
      imageUrls: imageUrl,
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isSent: true,
      showDate: false,
      createdAt: new Date().toISOString(),
      clientTimestamp: Date.now()
    };

    // 如果有引用消息，添加引用相关信息
    if (quotedMessage.value) {
      localMessage.quoteMessageId = quotedMessage.value.messageId;
      localMessage.quoteMessageContent = quotedMessage.value.text;
      localMessage.quoteMessageSenderName = quotedMessage.value.isSent ? 
        getUserInfo()?.userName : activeConversation.value.name;
      
      // 根据被引用消息类型设置quoteMessageType
      if (quotedMessage.value.type === 'image') {
        localMessage.quoteMessageType = 'image';
        localMessage.quoteMessageImageUrl = getImageUrl(quotedMessage.value.imageUrls || '');
      } else if (quotedMessage.value.type === 'file') {
        localMessage.quoteMessageType = 'file';
        localMessage.quoteMessageFileName = quotedMessage.value.fileName;
        localMessage.quoteMessageFileSize = quotedMessage.value.fileSize;
        localMessage.quoteMessageFileUrl = quotedMessage.value.fileUrl;
      } else {
        localMessage.quoteMessageType = 'text';
      }
    }

    // 立即添加到消息列表（优化用户体验）
    messages.value.push(localMessage);

    // 滚动到底部
    scrollToBottom();

    // 通过WebSocket发送消息
    if (globalWebSocketManager.isConnected()) {
      const messageToSend: SendMessage = {
        type: 'image',
        toTargetId: activeConversation.value.id,
        toTargetType: 'user',
        imageUrls: imageUrl,
        clientTimestamp: Date.now()
      };

      // 如果有引用消息，添加引用相关信息
      if (quotedMessage.value) {
        messageToSend.quoteMessageId = quotedMessage.value.messageId;
      }

      globalWebSocketManager.sendMessage(messageToSend);

      // 清除选中的图片和输入框以及引用消息
      selectedImage.value = null;
      newMessage.value = '';
      quotedMessage.value = null;
    } else {
      ElMessage.error('WebSocket未连接，无法发送消息');
      // 如果发送失败，从消息列表中移除本地消息
      const index = messages.value.findIndex((msg: Message) => msg.messageId === localMessage.messageId);
      if (index !== -1) {
        messages.value.splice(index, 1);
      }
    }
  } catch (error: any) {
    // 关闭加载提示
    ElMessage.closeAll();
    
    // 检查是否是认证错误
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      ElMessage.error('登录已过期，请重新登录');
      return;
    }

    ElMessage.error('发送图片失败: ' + (error.message || '未知错误'));
    // 清除选中的图片
    selectedImage.value = null;
  }
};

// 发送文件消息
const sendFileMessage = async (file: File) => {
  if (!activeConversation.value) {
    return;
  }

  try {
    // 显示上传提示
    const loadingMessage = ElMessage({
      message: '正在上传文件...',
      type: 'info',
      duration: 0 // 不自动关闭
    });

    let fileUrl;
    
    // 检查是否启用直传OSS功能
    if (import.meta.env.VITE_OSS_DIRECT_UPLOAD === 'true') {
      // 使用直传OSS
      fileUrl = await uploadFileToOSSDirectly(file, 'file');
    } else {
      // 使用原有的后端上传方式（保留作为备选方案）
      // 创建FormData对象用于上传文件
      const formData = new FormData();
      formData.append('file', file);
      formData.append('fileType', 'file');

      // 上传文件到服务器
      const response = await apiClient.post<string>(API_ENDPOINTS.FILE_UPLOAD, formData, {
        headers: {
          'Content-Type': 'multipart/form-data'
        }
      });

      // 注意：由于apiClient的响应拦截器，response已经是response.data（ResVO对象）
      // 所以我们可以直接访问code和msg属性
      if (response && response.code === 200) {
        fileUrl = response.data;
      } else {
        throw new Error(response?.msg || '上传失败');
      }
    }

    // 关闭加载提示
    loadingMessage.close();

    // 构造本地文件消息对象（用于立即显示）
    const localMessage: Message = {
      messageId: 'temp_' + Date.now().toString(), // 临时ID
      type: 'file',
      text: `[文件] ${file.name}`,
      fileUrl: fileUrl,
      fileName: file.name,
      fileSize: file.size || undefined, // 如果没有文件大小，则设为undefined
      time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }),
      isSent: true,
      showDate: false,
      createdAt: new Date().toISOString(),
      clientTimestamp: Date.now()
    };

    // 如果有引用消息，添加引用相关信息
    if (quotedMessage.value) {
      localMessage.quoteMessageId = quotedMessage.value.messageId;
      localMessage.quoteMessageContent = quotedMessage.value.text;
      localMessage.quoteMessageSenderName = quotedMessage.value.isSent ? 
        getUserInfo()?.userName : activeConversation.value.name;
      
      // 根据被引用消息类型设置quoteMessageType
      if (quotedMessage.value.type === 'image') {
        localMessage.quoteMessageType = 'image';
        localMessage.quoteMessageImageUrl = getImageUrl(quotedMessage.value.imageUrls || '');
      } else if (quotedMessage.value.type === 'file') {
        localMessage.quoteMessageType = 'file';
        localMessage.quoteMessageFileName = quotedMessage.value.fileName;
        localMessage.quoteMessageFileSize = quotedMessage.value.fileSize;
        localMessage.quoteMessageFileUrl = quotedMessage.value.fileUrl;
      } else {
        localMessage.quoteMessageType = 'text';
      }
    }

    // 立即添加到消息列表（优化用户体验）
    messages.value.push(localMessage);

    // 滚动到底部
    scrollToBottom();

    // 通过WebSocket发送消息
    if (globalWebSocketManager.isConnected()) {
      const messageToSend: SendMessage = {
        type: 'file',
        toTargetId: activeConversation.value.id,
        toTargetType: 'user',
        fileUrl: fileUrl,
        fileName: file.name,
        fileSize: file.size || undefined,
        textContent: `[文件] ${file.name}`,
        clientTimestamp: Date.now()
      };

      // 如果有引用消息，添加引用相关信息
      if (quotedMessage.value) {
        messageToSend.quoteMessageId = quotedMessage.value.messageId;
      }

      globalWebSocketManager.sendMessage(messageToSend);

      // 清除输入框和引用消息
      newMessage.value = '';
      quotedMessage.value = null;
    } else {
      ElMessage.error('WebSocket未连接，无法发送消息');
      // 如果发送失败，从消息列表中移除本地消息
      const index = messages.value.findIndex((msg: Message) => msg.messageId === localMessage.messageId);
      if (index !== -1) {
        messages.value.splice(index, 1);
      }
    }
  } catch (error: any) {
    // 关闭加载提示
    ElMessage.closeAll();
    
    // 检查是否是认证错误
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      ElMessage.error('登录已过期，请重新登录');
      return;
    }

    ElMessage.error('发送文件失败: ' + (error.message || '未知错误'));
  }
};

// 获取图片URL
const getImageUrl = (imageUrls: string): string => {
  // 现在直接返回图片URL字符串，而不是解析JSON数组
  return imageUrls || '';
};

// 预览图片
const previewImage = (url: string) => {
  if (url) {
    window.open(url, '_blank');
  }
};

// 下载文件
const downloadFile = (fileUrl: string, fileName: string) => {
  const link = document.createElement('a');
  link.href = fileUrl;
  link.download = fileName;
  link.style.display = 'none';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
};

// 格式化文件大小显示
const formatFileSize = (bytes: number) => {
  if (bytes === 0) return '0 Bytes';
  const k = 1024;
  const sizes = ['Bytes', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
};

// 滚动到底部
const scrollToBottom = () => {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

// 监听路由变化
watch(() => route.path, (newPath) => {
  if (newPath === '/chat') {
    // 检查是否有从好友列表传递过来的选中好友
    const selectedFriend = localStorage.getItem('selectedFriendForChat');
    if (selectedFriend) {
      try {
        const friend = JSON.parse(selectedFriend);
        // 创建一个临时会话对象
        const tempConversation = {
          id: friend.userId,
          name: friend.remark || friend.userName,
          avatar: getFullAvatarUrl(friend.avatarUrl) || '/src/assets/logo.svg',
          lastMessage: '',
          time: '',
          status: friend.loginStatus ? '在线' : '离线'
        };

        // 选择这个会话
        selectConversation(tempConversation);

        // 清除localStorage中的临时数据
        localStorage.removeItem('selectedFriendForChat');
      } catch (e) {
        console.error('解析选中好友信息失败:', e);
      }
    }
  }
});

// 加载会话列表
const loadConversations = async () => {
  try {
    const response = await apiClient.get(API_ENDPOINTS.FRIENDS_LIST)
    conversations.value = response.data.map((friend: any) => ({
      id: friend.userId,
      name: friend.remark || friend.userName,
      avatar: getFullAvatarUrl(friend.avatarUrl) || '/src/assets/logo.svg',
      lastMessage: '',
      time: '',
      status: friend.loginStatus ? '在线' : '离线'
    }))

    // 获取未读消息数量
    if (conversations.value.length > 0) {
      const friendIds = conversations.value.map((conv: any) => conv.id);
      const unreadResponse = await apiClient.get(API_ENDPOINTS.MESSAGE_UNREAD_COUNTS, {
        params: { friendIds }
      });

      if (unreadResponse && unreadResponse.data) {
        // 更新未读消息计数
        Object.entries(unreadResponse.data).forEach(([friendId, count]) => {
          const countNum = count as number;
          if (countNum > 0) {
            unreadCounts.value.set(friendId, countNum);
          }
        });
      }
    }
  } catch (error: any) {
    console.error('加载会话列表失败:', error)
    ElMessage.error('加载会话列表失败: ' + error.message)
  }
}

// 标记消息为已读
const markMessagesAsRead = async (friendId: string) => {
  try {
    await apiClient.post(API_ENDPOINTS.MESSAGE_MARK_AS_READ, null, {
      params: { friendId }
    });
  } catch (error) {
    console.error('标记消息为已读失败:', error);
  }
}

// 刷新未读消息数量
const refreshUnreadCounts = async () => {
  if (conversations.value.length > 0) {
    try {
      const friendIds = conversations.value.map((conv: any) => conv.id);
      const unreadResponse = await apiClient.get(API_ENDPOINTS.MESSAGE_UNREAD_COUNTS, {
        params: { friendIds }
      });

      if (unreadResponse && unreadResponse.data) {
        // 只更新未读计数大于0的会话，不清空其他已读会话的计数
        Object.entries(unreadResponse.data).forEach(([friendId, count]) => {
          const countNum = count as number;
          if (countNum > 0) {
            unreadCounts.value.set(friendId, countNum);
          } else {
            // 如果未读数为0，确保从Map中删除该会话
            unreadCounts.value.delete(friendId);
          }
        });
      }
    } catch (error) {
      console.error('刷新未读消息数量失败:', error);
    }
  }
}

// 组件挂载
onMounted(() => {
  // 监听全局用户状态变更事件
  window.addEventListener('userStatusChange', handleGlobalUserStatusChange);

  // 监听全局实时消息事件
  window.addEventListener('realTimeMessage', handleGlobalRealTimeMessage);

  // 加载会话列表
  loadConversations()

  // 检查并应用暗色模式
  const savedDarkMode = localStorage.getItem('userDarkMode')
  if (savedDarkMode === 'true') {
    document.body.classList.add('dark-mode')
  } else {
    // 确保在亮色模式下去除全局背景
    document.body.style.backgroundImage = 'none';
    document.body.classList.remove('login-page');
  }

  // 初始化背景设置
  updateBackgroundSettings()

  // 监听自定义背景透明度变化事件
  window.addEventListener('backgroundOpacityChanged', handleBackgroundOpacityChanged as EventListener);
  
  // 监听自定义背景图片变化事件
  window.addEventListener('backgroundImageChanged', handleBackgroundImageChanged as EventListener);
  
  // 监听暗色模式变化事件
  window.addEventListener('darkModeChanged', handleDarkModeChanged as EventListener);

  // 监听localStorage变化
  window.addEventListener('storage', handleStorageChanged);

  // 设置定时刷新未读消息数量
  unreadRefreshInterval.value = window.setInterval(() => {
    refreshUnreadCounts();
  }, 30000); // 每30秒刷新一次
})
// 组件卸载
onUnmounted(() => {
  // 移除全局用户状态变更事件监听器
  window.removeEventListener('userStatusChange', handleGlobalUserStatusChange);

  // 移除全局实时消息事件监听器
  window.removeEventListener('realTimeMessage', handleGlobalRealTimeMessage);

  // 清理事件监听器
  window.removeEventListener('backgroundOpacityChanged', handleBackgroundOpacityChanged as EventListener);
  window.removeEventListener('backgroundImageChanged', handleBackgroundImageChanged as EventListener);
  window.removeEventListener('darkModeChanged', handleDarkModeChanged as EventListener);
  window.removeEventListener('storage', handleStorageChanged);

  // 清理定时器
  if (unreadRefreshInterval.value) {
    clearInterval(unreadRefreshInterval.value);
  }
})
// 组件激活时检查是否有需要选择的好友
onActivated(() => {
  // 检查是否有从好友列表传递过来的选中好友
  const selectedFriend = localStorage.getItem('selectedFriendForChat');
  if (selectedFriend) {
    try {
      const friend = JSON.parse(selectedFriend);
      // 创建一个临时会话对象
      const tempConversation = {
        id: friend.userId,
        name: friend.remark || friend.userName,
        avatar: getFullAvatarUrl(friend.avatarUrl) || '/src/assets/logo.svg',
        lastMessage: '',
        time: '',
        status: friend.loginStatus ? '在线' : '离线'
      };

      // 选择这个会话
      selectConversation(tempConversation);

      // 清除localStorage中的临时数据
      localStorage.removeItem('selectedFriendForChat');
    } catch (e) {
      console.error('解析选中好友信息失败:', e);
    }
  }
});
// 处理全局用户状态变更事件
const handleGlobalUserStatusChange = (event: Event) => {
  console.log('ChatView收到用户状态变更事件');
  const customEvent = event as CustomEvent;
  handleUserStatusChange(customEvent.detail);
};

// 处理全局实时消息事件
const handleGlobalRealTimeMessage = (event: Event) => {
  console.log('ChatView收到实时消息事件');
  const customEvent = event as CustomEvent;
  handleRealTimeMessage(customEvent.detail);
};
</script>






<template>
  <div class="chat-view">
    <div class="chat-container">
      <div class="chat-sidebar">
        <div class="chat-sidebar-header">
          <h2>聊天</h2>
        </div>
        
        <div class="conversations-list">
          <div 
            v-for="conversation in conversations" 
            :key="conversation.id"
            class="conversation-item"
            :class="{ active: activeConversation?.id === conversation.id }"
            @click="selectConversation(conversation)"
          >
            <el-avatar :src="getFullAvatarUrl(conversation.avatar)" />
            <div class="conversation-info">
              <div class="conversation-name">{{ conversation.name }}</div>
              <div class="conversation-last-message">{{ conversation.lastMessage }}</div>
            </div>
            <div class="conversation-time">{{ conversation.time }}</div>
            <div class="conversation-status" :class="{ online: conversation.status === '在线' }"></div>
            <!-- 未读消息红点 -->
            <div v-if="(unreadCounts.get(conversation.id) || 0) > 0" class="unread-badge">
              {{ (unreadCounts.get(conversation.id) || 0) > 99 ? '99+' : (unreadCounts.get(conversation.id) || 0) }}
            </div>
          </div>
        </div>
      </div>
      
      <div class="chat-main">
        <div class="chat-header" v-if="activeConversation">
          <div class="chat-header-info">
            <el-avatar :size="40" :src="getFullAvatarUrl(activeConversation.avatar)" />
            <div class="chat-header-text">
              <div class="chat-participant-name">{{ activeConversation.name }}</div>
              <div class="chat-status" :class="activeConversation.status === '在线' ? 'status-online' : 'status-offline'">{{ activeConversation.status }}</div>
            </div>
          </div>
          <div class="chat-header-actions">
            <el-button icon="el-icon-more" circle></el-button>
          </div>
        </div>
        

        
        <!-- 背景容器 -->
        <div class="chat-background-container" v-if="activeConversation">
          <!-- 背景图片层 -->
          <div 
            class="message-background" 
            v-if="backgroundSettings.backgroundImage"
            :style="backgroundStyle"
          ></div>
          <!-- 覆盖层用于控制透明度 -->
          <div 
            class="message-overlay" 
            v-if="backgroundSettings.backgroundImage"
            :style="overlayStyle"
          ></div>
          
          <div class="chat-messages-container" ref="messagesContainer">
            <div class="chat-messages">
              <template v-for="message in messages" :key="message.messageId">
                <div v-if="message.showDate" class="message-date-divider">
                  <span>{{ formatDate(message.createdAt) }}</span>
                </div>
                <div 
                  class="message"
                  :class="{ 'sent': message.isSent, 'received': !message.isSent }"
                  @touchstart.prevent="setQuotedMessage(message)"
                  @contextmenu.prevent="setQuotedMessage(message)"
                >
                  <el-avatar v-if="!message.isSent" :src="getFullAvatarUrl(activeConversation.avatar)" class="message-avatar" />
                  <div class="message-content">
                    <div class="message-sender-placeholder" v-if="message.isSent"></div>
                    <div class="message-sender" v-if="!message.isSent">{{ activeConversation.name }}</div>
                    
                    <!-- 显示文本消息 -->
                    <div v-if="message.type === 'text'" class="message-text">
                      <div v-if="message.quoteMessageId" class="quote-message">
                        <div class="quote-sender">@{{ message.quoteMessageSenderName }}</div>
                        <div class="quote-content">{{ message.quoteMessageContent }}</div>
                      </div>
                      <div>{{ message.text }}</div>
                    </div>

                    <!-- 显示引用消息 -->
                    <div v-else-if="message.type === 'quote'" class="message-text">
                      <div class="quote-message">
                        <div class="quote-sender">@{{ message.quoteMessageSenderName }}</div>
                        <!-- 根据被引用消息类型显示不同内容 -->
                        <div v-if="message.quoteMessageType === 'image'" class="quoted-image-preview">
                          <img
                              :src="message.quoteMessageImageUrl"
                              alt="引用的图片"
                              class="quoted-image"
                              @click="previewImage(message.quoteMessageImageUrl)"
                          />
                          <div class="quoted-label">[图片]</div>
                        </div>
                        <div v-else-if="message.quoteMessageType === 'file'" class="quoted-file-preview">
                          <div class="file-icon">📁</div>
                          <div class="file-info">
                            <div class="file-name">{{ message.quoteMessageFileName }}</div>
                            <div class="file-size">{{ formatFileSize(message.quoteMessageFileSize) }}</div>
                          </div>
                          <div class="quoted-label">[文件]</div>
                        </div>
                        <div v-else class="quote-content">{{ message.quoteMessageContent }}</div>
                      </div>
                      <div>{{ message.text }}</div>
                    </div>
                    <!-- 显示图片消息 -->
                    <div v-else-if="message.type === 'image'" class="message-image">
                      <div v-if="message.quoteMessageId" class="quote-message">
                        <div class="quote-sender">@{{ message.quoteMessageSenderName }}</div>
                        <div class="quote-content">{{ message.quoteMessageContent }}</div>
                      </div>
                      <img :src="getImageUrl(message.imageUrls || '')" alt="图片消息" @click="previewImage(getImageUrl(message.imageUrls || ''))" />
                    </div>
                    
                    <!-- 显示文件消息 -->
                    <div v-else-if="message.type === 'file'" class="message-file">
                      <div v-if="message.quoteMessageId" class="quote-message">
                        <div class="quote-sender">@{{ message.quoteMessageSenderName }}</div>
                        <div class="quote-content">{{ message.quoteMessageContent }}</div>
                      </div>
                      <div class="file-icon">📁</div>
                      <div class="file-info">
                        <div class="file-name" @click="downloadFile(message.fileUrl || '', message.fileName || '')">{{ message.fileName }}</div>
                        <div class="file-size">{{ formatFileSize(message.fileSize || 0) }}</div>
                      </div>
                      <el-button size="small" @click="downloadFile(message.fileUrl || '', message.fileName || '')">下载</el-button>
                    </div>
                    
                    <!-- 其他类型消息默认显示文本 -->
                    <div v-else class="message-text">{{ message.text }}</div>
                    <div class="message-time">{{ message.time }}</div>
                  </div>
                  <el-avatar v-if="message.isSent" :src="getFullAvatarUrl(getUserInfo()?.avatarUrl || '')" class="message-avatar" />
                </div>
              </template>
            </div>
          </div>
          
          <div class="chat-input-area" v-if="activeConversation">
            <div class="input-container-wrapper">
              <!-- 折叠按钮 -->
              <button 
                v-if="activeConversation"
                @click="toggleAttachmentToolbar"
                class="icon-button toggle-attachment-button"
                :class="{ 'expanded': isAttachmentToolbarOpen }"
                title="附件工具">
                <span v-if="!isAttachmentToolbarOpen">+</span>
                <span v-else>−</span>
              </button>
          
              <div class="input-and-toolbar-container">
                <!-- 附件工具栏（默认隐藏） -->
                <div v-show="isAttachmentToolbarOpen && activeConversation" class="chat-input-tools">
                  <button 
                    @click="selectImage"
                    class="icon-button"
                    title="发送图片">
                    📷
                  </button>
                  <button 
                    @click="selectFile"
                    class="icon-button"
                    title="发送文件">
                    📎
                  </button>
                </div>
                
                <div class="input-container">
                  <!-- 引用消息预览 -->
                  <div v-if="quotedMessage" class="quote-preview">
                    <div class="quote-preview-content">
                      <div class="quote-sender">@{{ quotedMessage.isSent ? getUserInfo()?.userName : activeConversation.name }}</div>
                      <!-- 根据被引用消息类型显示不同内容 -->
                      <div v-if="quotedMessage.type === 'image'" class="quoted-preview-image">
                        <img 
                          :src="getImageUrl(quotedMessage.imageUrls||'')"
                          alt="引用的图片" 
                          class="quoted-preview-image-thumb"
                          @click="previewImage(getImageUrl(quotedMessage.imageUrls||''))"
                        />
                        <div class="quoted-preview-label">[图片]</div>
                      </div>
                      <div v-else-if="quotedMessage.type === 'file'" class="quoted-preview-file">
                        <div class="file-icon">📁</div>
                        <div class="file-info">
                          <div class="file-name">{{ quotedMessage.fileName }}</div>
                          <div class="file-size">{{ formatFileSize(quotedMessage.fileSize||0) }}</div>
                        </div>
                      </div>
                      <div v-else class="quote-text">{{ quotedMessage.text }}</div>
                    </div>
                    <button @click="cancelQuotedMessage" class="cancel-quote">×</button>
                  </div>
                  
                  <el-input
                    v-model="newMessage"
                    type="textarea"
                    :rows="2"
                    placeholder="输入消息..."
                    @keyup.enter="sendMessage"
                    class="message-input"
                  ></el-input>
                  <div class="chat-input-actions">
                    <el-button 
                      type="primary" 
                      @click="sendMessage"
                      :disabled="!newMessage.trim()"
                      size="small"
                    >
                      发送
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </div>
          
          <div class="chat-placeholder" v-else>
            <div class="placeholder-content">
              <i class="el-icon-chat-dot-round placeholder-icon"></i>
              <p>选择一个聊天或开始新对话</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>


<style scoped>
.chat-view {
  height: 100%;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
  color: #1a1a1a;
}

.chat-container {
  display: flex;
  flex: 1;
  min-height: 0;
}

.chat-sidebar {
  width: 300px;
  border-right: 1px solid #ddd;
  display: flex;
  flex-direction: column;
  background-color: #ffffff;
}

.dark-mode .chat-sidebar {
  border-right: 1px solid #444;
  background-color: #1a1a1a;
}

.chat-sidebar-header {
  padding: 20px;
  border-bottom: 1px solid #ddd;
  background-color: #ffffff;
}

.dark-mode .chat-sidebar-header {
  border-bottom: 1px solid #444;
  background-color: #2d2d2d;
}

.chat-sidebar-header h2 {
  margin: 0;
  font-size: 18px;
  color: #1a1a1a;
}

.dark-mode .chat-sidebar-header h2 {
  color: #f5f5f5;
}

.conversations-list {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  padding: 15px 20px;
  cursor: pointer;
  position: relative;
  border-bottom: 1px solid #eee;
  background-color: #ffffff;
}

.dark-mode .conversation-item {
  border-bottom: 1px solid #333;
  background-color: #1a1a1a;
}

.conversation-item:hover {
  background-color: #f9f9f9;
}

.dark-mode .conversation-item:hover {
  background-color: #2a2a2a;
}

.conversation-item.active {
  background-color: #e0f0ff;
}

.dark-mode .conversation-item.active {
  background-color: #1a3a4a;
}

.conversation-item .el-avatar {
  margin-right: 15px;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-name {
  font-weight: 500;
  margin-bottom: 4px;
  color: #1a1a1a;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .conversation-name {
  color: #ffffff;
}

.conversation-last-message {
  font-size: 12px;
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .conversation-last-message {
  color: #e0e0e0;
}

.conversation-time {
  font-size: 12px;
  color: #666;
  margin-right: 10px;
}

.dark-mode .conversation-time {
  color: #e0e0e0;
}

.conversation-status {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background-color: #ccc;
  margin-right: 5px;
}

.conversation-status.online {
  background-color: #4caf50;
}

/* 未读消息红点 */
.unread-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: #f56c6c;
  color: white;
  border-radius: 10px;
  padding: 0 5px;
  font-size: 12px;
  min-width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  position: relative;
  background-color: #ffffff;
}

.dark-mode .chat-main {
  background-color: #1a1a1a;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 20px;
  border-bottom: 1px solid #ddd;
  background-color: #ffffff;
}

.dark-mode .chat-header {
  border-bottom: 1px solid #444;
  background-color: #2d2d2d;
}

.chat-header-info {
  display: flex;
  align-items: center;
}

.chat-header-text {
  margin-left: 15px;
}

.chat-participant-name {
  font-weight: 500;
  font-size: 16px;
  color: #1a1a1a;
}

.dark-mode .chat-participant-name {
  color: #ffffff;
}

.chat-status {
  font-size: 12px;
}

.chat-status.status-online {
  color: #67c23a; /* 绿色 */
}

.chat-status.status-offline {
  color: #909399; /* 灰色 */
}

.dark-mode .chat-status.status-online {
  color: #67c23a; /* 绿色 */
}

.dark-mode .chat-status.status-offline {
  color: #909399; /* 灰色 */
}

.dark-mode .chat-status {
  color: #d0d0d0;
}

.chat-background-container {
  flex: 1;
  position: relative;
  overflow: hidden;
  background-color: #ffffff; /* 添加默认背景色 */
}

.dark-mode .chat-background-container {
  background-color: #1a1a1a; /* 暗色模式下的背景色 */
}

.chat-messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  position: relative;
  z-index: 2;
  height: calc(100vh - 200px);
}

.chat-messages {
  display: flex;
  flex-direction: column;
}

.message-date-divider {
  text-align: center;
  padding: 10px 0;
  color: #666;
  font-size: 12px;
  position: relative;
}

.message-date-divider::before {
  content: "";
  position: absolute;
  top: 100%;
  left: 25%;
  right: 25%;
  height: 1px;
  background: linear-gradient(to right, transparent, #999, transparent);
  z-index: 1;
}

.message-date-divider span {
  position: relative;
  padding: 4px 12px;
  z-index: 2;
  font-size: 12px;
}

.dark-mode .message-date-divider span {
  color: #ffffff;
}

.message {
  display: flex;
  margin-bottom: 15px;
  max-width: 100%;
  position: relative;
}

.message.received {
  align-self: flex-start;
  max-width: 40%;
}

.message.sent {
  align-self: flex-end;
  flex-direction: row;
  margin-left: auto;
  max-width: 40%;
}

.message-avatar {
  flex-shrink: 0;
  align-self: flex-start;
  margin-top: -5px;
}

.message.received .message-avatar {
  margin-right: 10px;
}

.message.sent .message-avatar {
  order: 2;
  margin-left: 10px;
}

.message-content {
  flex: 1;
  min-width: 0;
  max-width: 100%;
}

.message-sender-placeholder {
  height: 16px;
  margin-bottom: 4px;
}

.message-sender {
  font-size: 12px;
  color: #666;
  margin-bottom: 4px;
}

.dark-mode .message-sender {
  color: #e0e0e0;
}

.message-text {
  padding: 10px 15px;
  border-radius: 18px;
  word-wrap: break-word;
  max-width: 100%;
  color: #1a1a1a;
  white-space: pre-wrap; /* 保持原始空格和换行符格式 */
}

.message-image {
  max-width: 100%;
  border-radius: 10px;
  overflow: hidden;
  margin: 5px 0;
  position: relative;
  display: inline-block;
}

.message-image img {
  max-width: 100%;
  max-height: 200px;
  border-radius: 10px;
  cursor: pointer;
  transition: transform 0.2s;
}

.message-image img:hover {
  transform: scale(1.05);
}

.dark-mode .message-text {
  color: #ffffff;
  white-space: pre-wrap; /* 保持原始空格和换行符格式 */
}

.message.received .message-text {
  background-color: #f5f5f5;
  border-top-left-radius: 4px;
  color: #1a1a1a;
}

.dark-mode .message.received .message-text {
  background-color: #333;
  color: #ffffff;
}

.message.sent .message-text {
  background-color: #409eff;
  color: white;
  border-top-right-radius: 4px;
}

.message-time {
  font-size: 12px;
  color: #666;
  margin-top: 4px;
  align-self: flex-start;
  text-align: left;
}

.message.received .message-time {
  align-self: flex-start;
  text-align: left;
}

.message.sent .message-time {
  align-self: flex-end;
  text-align: right;
}

.dark-mode .message-time {
  color: #e0e0e0;
}

.chat-input-area {
  padding: 15px;
  border-top: 1px solid #ddd;
  background-color: #ffffff;
  position: relative;
  z-index: 2;
}

.dark-mode .chat-input-area {
  border-top: 1px solid #444;
  background-color: #1a1a1a;
}

/* Element Plus 输入框在暗色模式下的样式 */
.dark-mode .message-input :deep(.el-textarea__inner) {
  background-color: #1a1a1a;
  border-color: #444;
  color: #f5f5f5;
}

.dark-mode .message-input :deep(.el-textarea__inner:focus) {
  border-color: #409eff;
}

.dark-mode .message-input :deep(.el-textarea .el-input__count) {
  color: #ccc;
}

.chat-input-tools-wrapper {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.toggle-attachment-button {
  margin-right: 8px;
}

.chat-input-tools {
  display: flex;
  gap: 8px;
}

.icon-button {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #dcdfe6;
  background-color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.3s;
  color: #606266;
  line-height: 1; /* 确保图标垂直居中 */
}

.icon-button:hover {
  background: linear-gradient(135deg, #000, #fff); /* 渐变背景色 */
  color: white;
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3); /* 添加阴影效果 */
}

.dark-mode .icon-button {
  background-color: #2d2d2d;
  border-color: #444;
  color: #ccc;
}

.dark-mode .icon-button:hover {
  background: linear-gradient(135deg, #fff, #000);
  color: white;
  border-color: #409eff;
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.3);
}

.message-input {
  margin-bottom: 10px;
}

.chat-input-actions {
  display: flex;
  justify-content: flex-end;
}

.chat-placeholder {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
}

.dark-mode .chat-placeholder {
  background-color: #1a1a1a;
}

.placeholder-content {
  text-align: center;
  color: #999;
}

.dark-mode .placeholder-content {
  color: #ccc;
  background-color: #1a1a1a;
}

.placeholder-icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.message-file {
  display: flex;
  align-items: center;
  padding: 10px;
  background-color: #f5f5f5;
  border-radius: 8px;
  max-width: 300px;
}

.dark-mode .message-file {
  background-color: #333;
}

.file-icon {
  font-size: 24px;
  margin-right: 10px;
}

.file-info {
  flex: 1;
  min-width: 0;
}

.file-name {
  font-size: 14px;
  color: #1a1a1a;
  cursor: pointer;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .file-name {
  color: #ffffff;
}

.file-size {
  font-size: 12px;
  color: #666;
  margin-top: 2px;
}

.dark-mode .file-size {
  color: #e0e0e0;
}

.attachment-toolbar {
  display: flex;
  gap: 8px;
  margin-right: 8px;
}

.icon-button.expanded {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.dark-mode .icon-button.expanded {
  background: linear-gradient(135deg, #764ba2, #667eea);
  color: white;
  border-color: #764ba2;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.input-container-wrapper {
  display: flex;
  align-items: flex-end;
  position: relative;
}

.input-and-toolbar-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-input-tools {
  display: flex;
  gap: 8px;
  position: absolute;
  bottom: 100%;
  left: -15px;
  z-index: 10;
  background-color: #ffffff;
  padding: 5px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.dark-mode .chat-input-tools {
  background-color: #2d2d2d;
}

.toggle-attachment-button {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  border: 1px solid #dcdfe6;
  background-color: #ffffff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  transition: all 0.3s;
  color: #606266;
  line-height: 1;
  margin-right: 8px;
  flex-shrink: 0;
}

.toggle-attachment-button:hover {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.dark-mode .toggle-attachment-button {
  background-color: #2d2d2d;
  border-color: #444;
  color: #ccc;
}

.dark-mode .toggle-attachment-button:hover {
  background: linear-gradient(135deg, #764ba2, #667eea);
  color: white;
  border-color: #764ba2;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.icon-button.expanded {
  background: linear-gradient(135deg, #667eea, #764ba2);
  color: white;
  border-color: #667eea;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.dark-mode .icon-button.expanded {
  background: linear-gradient(135deg, #764ba2, #667eea);
  color: white;
  border-color: #764ba2;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.input-container {
  display: flex;
  align-items: center;
  gap: 10px;
  /* 移除input-container的上边距，避免产生细线 */
  margin-top: 0;
  /* 确保容器紧贴上方元素 */
  padding-top: 0;
}

/* 调整消息输入框的样式，移除可能的上边框 */
.message-input {
  flex: 1;
  /* 移除可能的上边距 */
  margin-top: 0;
}

/* 调整Element Plus textarea的样式 */
.message-input :deep(.el-textarea__inner) {
  /* 移除上边框 */
  border-top: none;
  /* 确保没有上边距 */
  margin-top: 0;
}

.dark-mode .message-input :deep(.el-textarea__inner) {
  background-color: #1a1a1a;
  border-color: #444;
  color: #f5f5f5;
  /* 在暗色模式下也移除上边框 */
  border-top: none;
}

.dark-mode .message-input :deep(.el-textarea__inner:focus) {
  border-color: #409eff;
  /* 聚焦时也保持上边框移除 */
  border-top: none;
}

.chat-input-actions {
  margin-bottom: 5px;
}

.quoted-image-preview {
  margin-top: 5px;
  border: 1px solid #eee;
  border-radius: 4px;
  overflow: hidden;
  max-width: 150px;
  position: relative;
  display: inline-block;
}

.dark-mode .quoted-image-preview {
  border: 1px solid #444;
}

.quoted-image-preview .quoted-image {
  max-width: 100%;
  max-height: 100px;
  object-fit: cover;
  cursor: pointer;
}

.quoted-image-preview .quoted-label {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  font-size: 12px;
  text-align: center;
  padding: 2px;
}

.quoted-file-preview {
  display: flex;
  align-items: center;
  padding: 6px;
  background-color: #f5f5f5;
  border-radius: 4px;
  margin-top: 5px;
  max-width: 250px;
}

.dark-mode .quoted-file-preview {
  background-color: #444;
}

.quoted-file-preview .file-icon {
  font-size: 16px;
  margin-right: 6px;
}

.quoted-file-preview .file-info {
  flex: 1;
  min-width: 0;
}

.quoted-file-preview .file-name {
  font-size: 12px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .quoted-file-preview .file-name {
  color: #fff;
}

.quoted-file-preview .file-size {
  font-size: 11px;
  color: #666;
  margin-top: 2px;
}

.dark-mode .quoted-file-preview .file-size {
  color: #ccc;
}

.quote-message {
  background-color: #f0f0f0;
  border-left: 3px solid #0084ff;
  padding: 5px 10px;
  margin-bottom: 5px;
  border-radius: 4px;
  font-size: 12px;
}

.dark-mode .quote-message {
  background-color: #333;
  border-left-color: #0077e6;
}

.quote-message .quote-sender {
  font-weight: bold;
  color: #0084ff;
  margin-bottom: 2px;
}

.quote-message .quote-content {
  color: #666;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .quote-content {
  color: #ccc;
}

.quote-preview {
  background-color: #f0f0f0;
  border-left: 3px solid #0084ff;
  padding: 8px 12px;
  margin-bottom: 8px;
  margin-top: 0;
  border-radius: 4px;
  font-size: 13px;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.dark-mode .quote-preview {
  background-color: #333;
  border-left-color: #0077e6;
}

.quote-preview-content {
  flex: 1;
  min-width: 0;
  max-width: 300px;
}

.quote-preview .quote-sender {
  font-weight: bold;
  color: #0084ff;
  margin-bottom: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.quote-preview .quote-text {
  color: #666;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.dark-mode .quote-preview .quote-sender {
  color: #409eff;
}

.dark-mode .quote-preview .quote-text {
  color: #ccc;
}

.cancel-quote {
  background: none;
  border: none;
  font-size: 18px;
  cursor: pointer;
  color: #999;
  padding: 0;
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  margin-left: 8px;
  flex-shrink: 0;
}

.cancel-quote:hover {
  background-color: #e0e0e0;
  color: #666;
}

.dark-mode .cancel-quote {
  color: #aaa;
}

.dark-mode .cancel-quote:hover {
  background-color: #444;
  color: #fff;
}

/* 引用预览中的图片样式 */
.quoted-preview-image {
  position: relative;
  display: inline-block;
  max-width: 100px;
  margin-top: 5px;
}

.quoted-preview-image-thumb {
  max-width: 100%;
  max-height: 60px;
  border-radius: 4px;
  cursor: pointer;
  object-fit: cover;
}

.quoted-preview-label {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: rgba(0, 0, 0, 0.5);
  color: white;
  font-size: 12px;
  text-align: center;
  padding: 2px;
  border-radius: 0 0 4px 4px;
}

/* 引用预览中的文件样式 */
.quoted-preview-file {
  display: flex;
  align-items: center;
  margin-top: 5px;
  background-color: #e0e0e0;
  padding: 5px;
  border-radius: 4px;
}

.dark-mode .quoted-preview-file {
  background-color: #444;
}

.quoted-preview-file .file-icon {
  font-size: 16px;
  margin-right: 5px;
}

.quoted-preview-file .file-info {
  flex: 1;
  min-width: 0;
}

.quoted-preview-file .file-name {
  font-size: 12px;
  color: #333;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.dark-mode .quoted-preview-file .file-name {
  color: #fff;
}

.quoted-preview-file .file-size {
  font-size: 11px;
  color: #666;
  margin-top: 2px;
}

.dark-mode .quoted-preview-file .file-size {
  color: #ccc;
}

</style>