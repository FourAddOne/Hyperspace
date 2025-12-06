import { Client} from '@stomp/stompjs';
import type { StompSubscription } from '@stomp/stompjs';
import { getAccessToken, getRefreshToken, saveTokens, removeTokens } from '@/utils/auth';
import { removeUserInfo } from '@/utils/user';
import apiClient, { API_ENDPOINTS } from '@/services/api';

// 简单的日志级别控制
const isDevelopment = process.env.NODE_ENV === 'development';

const log = {
  debug: (...args: any[]) => {
    if (isDevelopment) {
      console.debug('[WebSocket][DEBUG]', ...args);
    }
  },
  info: (...args: any[]) => {
    if (isDevelopment) {
      console.info('[WebSocket][INFO]', ...args);
    }
  },
  warn: (...args: any[]) => {
    if (isDevelopment) {
      console.warn('[WebSocket][WARN]', ...args);
    }
  },
  error: (...args: any[]) => {
    if (isDevelopment) {
      console.error('[WebSocket][ERROR]', ...args);
    }
  }
};

// Base64解码函数
function base64Decode(base64String: string): string {
  // 使用window.atob进行解码，并通过类型断言解决类型问题
  return decodeURIComponent(escape(window.atob(base64String)));
}

class WebSocketService {
  private client: Client | null = null;
  private subscriptions: Map<string, StompSubscription> = new Map();
  private connected = false;
  private userStatusCallback: ((data: any) => void) | null = null;
  private messageCallback: ((data: any) => void) | null = null;
  private reconnectTimeout: any = null;
  private maxReconnectAttempts = 5;
  private reconnectAttempts = 0;

  // 检查并刷新令牌
  private async checkAndRefreshToken(): Promise<string | null> {
    const accessToken = getAccessToken();
    const refreshToken = getRefreshToken();

    if (!accessToken || !refreshToken) {
      log.error('缺少访问令牌或刷新令牌');
      // 通知应用需要重新登录
      window.dispatchEvent(new CustomEvent('auth-expired'));
      return null;
    }

    // 解析JWT令牌以检查过期时间
    try {
      // 修复atob类型问题
      const tokenParts = accessToken.split('.');
      if (tokenParts.length !== 3) {
        throw new Error('无效的JWT令牌格式');
      }
      
      const payloadBase64 = tokenParts[1];
      // 检查payloadBase64是否为undefined
      if (!payloadBase64) {
        throw new Error('JWT令牌负载部分为空');
      }
      
      // 使用自定义函数代替atob以避免类型问题
      const payloadString = base64Decode(payloadBase64);
      const payload = JSON.parse(payloadString);
      const exp = payload.exp;
      const now = Math.floor(Date.now() / 1000);

      // 如果令牌已经过期或者将在1分钟内过期，则刷新
      if (exp <= now + 60) {
        log.info('访问令牌即将过期，尝试刷新令牌');

        try {
          const response = await apiClient.post(API_ENDPOINTS.USER_REFRESH, {}, {
            headers: {
              Authorization: `Bearer ${refreshToken}`
            }
          });

          if (response && response.code === 200) {
            const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data;
            saveTokens(newAccessToken, newRefreshToken);
            log.info('令牌刷新成功');
            return newAccessToken;
          } else {
            log.error('刷新令牌失败:', response?.msg);
            // 刷新令牌失败，清除令牌并通知应用需要重新登录
            removeTokens();
            removeUserInfo();
            window.dispatchEvent(new CustomEvent('auth-expired'));
            return null;
          }
        } catch (error) {
          log.error('刷新令牌时发生错误:', error);
          // 刷新令牌失败，清除令牌并通知应用需要重新登录
          removeTokens();
          removeUserInfo();
          window.dispatchEvent(new CustomEvent('auth-expired'));
          return null;
        }
      } else {
        // 令牌仍然有效
        return accessToken;
      }
    } catch (error) {
      log.error('解析JWT令牌时出错:', error);
      // 解析令牌出错，清除令牌并通知应用需要重新登录
      removeTokens();
      removeUserInfo();
      window.dispatchEvent(new CustomEvent('auth-expired'));
      return null;
    }
  }

  // 连接到WebSocket服务器
  async connect(
    onUserStatusChange: (data: any) => void, 
    onMessageReceived?: (data: any) => void
  ) {
    // 如果已经连接并且回调函数没有变化，则不需要重新连接
    if (this.connected && 
        this.userStatusCallback === onUserStatusChange && 
        this.messageCallback === onMessageReceived) {
      log.debug('WebSocket已经连接且回调函数未变化，无需重新连接');
      return;
    }
    
    // 如果已经连接但回调函数有变化，只需要更新回调函数和重新订阅
    if (this.connected) {
      log.debug('WebSocket已连接，更新回调函数');
      this.userStatusCallback = onUserStatusChange;
      if (onMessageReceived) {
        this.messageCallback = onMessageReceived;
      }
      // 重新订阅以使用新的回调函数
      this.resubscribe();
      return;
    }
    
    this.userStatusCallback = onUserStatusChange;
    if (onMessageReceived) {
      this.messageCallback = onMessageReceived;
    }
    
    // 检查并刷新令牌
    const token = await this.checkAndRefreshToken();
    if (!token) {
      log.error('无法连接WebSocket: 无法获取有效的访问令牌');
      // 不再安排重新连接，因为已经触发了auth-expired事件
      // this.scheduleReconnect();
      return;
    }

    // 创建STOMP客户端
    this.client = new Client({
      brokerURL: 'ws://localhost:8080/ws', // 使用WebSocket URL
      connectHeaders: {
        Authorization: `Bearer ${token}`
      },
      // 启用STOMP客户端调试模式
      debug: function (str) {
        // 生产环境不输出STOMP调试日志
      },
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
    });

    // console.log('[WebSocket] STOMP客户端配置:', {
    //   brokerURL: 'ws://localhost:8080/ws',
    //   connectHeaders: {
    //     Authorization: `Bearer ${token}`
    //   }
    // });

    // 连接成功回调
    this.client.onConnect = () => {
      log.info('WebSocket连接成功');
      this.connected = true;
      this.reconnectAttempts = 0; // 重置重连尝试次数

      // 订阅用户状态变更消息
      this.subscribeToUserStatus();

      // 如果提供了消息回调，订阅聊天消息
      if (this.messageCallback) {
        this.subscribeToMessages();
      }
    };

    // 连接错误回调
    this.client.onStompError = (frame) => {
      log.error('WebSocket连接错误: ', frame.headers['message']);
      log.debug('详细错误信息: ', frame.body);
      this.handleConnectionError();
    };
    
    // WebSocket断开连接回调
    this.client.onDisconnect = () => {
      log.info('WebSocket断开连接');
      this.connected = false;
      this.handleConnectionError();
    };

    // WebSocket连接失败回调
    this.client.onWebSocketError = (event) => {
      log.error('WebSocket连接失败:', event);
      this.handleConnectionError();
    };

    // 启动连接
    log.debug('激活WebSocket客户端');
    this.client.activate();
  }

  // 处理连接错误
  private handleConnectionError() {
    this.connected = false;
    this.scheduleReconnect();
  }

  // 安排重新连接
  private scheduleReconnect() {
    if (this.reconnectAttempts < this.maxReconnectAttempts) {
      this.reconnectAttempts++;
      log.info(`尝试重新连接 (${this.reconnectAttempts}/${this.maxReconnectAttempts})...`);
      
      // 清除之前的重连定时器
      if (this.reconnectTimeout) {
        clearTimeout(this.reconnectTimeout);
      }
      
      // 设置重新连接定时器
      this.reconnectTimeout = setTimeout(() => {
        log.debug('尝试重新连接WebSocket...');
        // 修复类型错误，确保传递正确的参数类型
        if (this.userStatusCallback) {
          this.connect(this.userStatusCallback, this.messageCallback || undefined);
        } else {
          // 如果userStatusCallback为null，创建一个空函数作为默认回调
          this.connect(() => {}, this.messageCallback || undefined);
        }
      }, 3000 * this.reconnectAttempts); // 指数退避策略
    } else {
      log.error('达到最大重连尝试次数，停止重连');
    }
  }

  // 更新用户状态回调函数
  updateUserStatusCallback(callback: (data: any) => void) {
    this.userStatusCallback = callback;
  }

  // 更新消息回调函数
  updateMessageCallback(callback: (data: any) => void) {
    this.messageCallback = callback;
    // 如果已经连接，重新订阅消息以使用新的回调函数
    if (this.connected) {
      this.resubscribe();
    }
  }

  // 重新订阅主题
  private resubscribe() {
    log.debug('重新订阅WebSocket主题');
    // 取消现有订阅
    this.subscriptions.forEach((subscription, destination) => {
      subscription.unsubscribe();
    });
    this.subscriptions.clear();

    // 重新订阅
    this.subscribeToUserStatus();
    if (this.messageCallback) {
      this.subscribeToMessages();
    }
  }

  // 订阅用户状态变更消息
  private subscribeToUserStatus() {
    if (!this.client) return;
    
    log.debug('订阅用户状态变更消息: /topic/user-status');
    const userStatusSubscription = this.client.subscribe('/topic/user-status', (message) => {
      try {
        const data = JSON.parse(message.body);
        log.debug('收到用户状态变更消息:', data);
        if (this.userStatusCallback) {
          this.userStatusCallback(data);
        }
      } catch (error) {
        log.error('解析用户状态消息失败:', error);
      }
    });

    if (userStatusSubscription) {
      this.subscriptions.set('/topic/user-status', userStatusSubscription);
    }
  }

  // 订阅聊天消息
  private subscribeToMessages() {
    if (!this.client || !this.messageCallback) return;
    
    log.debug('订阅聊天消息: /user/queue/messages');
    const messageSubscription = this.client.subscribe('/user/queue/messages', (message) => {
      try {
        const data = JSON.parse(message.body);
        log.debug('收到聊天消息:', data);
        if (this.messageCallback) {
          this.messageCallback(data);
        }
      } catch (error) {
        log.error('解析聊天消息失败:', error);
      }
    });

    if (messageSubscription) {
      this.subscriptions.set('/user/queue/messages', messageSubscription);
    }
  }

  // 断开连接
  disconnect() {
    // 清除重连定时器
    if (this.reconnectTimeout) {
      clearTimeout(this.reconnectTimeout);
      this.reconnectTimeout = null;
    }
    
    if (this.client) {
      log.info('断开WebSocket连接');
      // 取消所有订阅
      this.subscriptions.forEach((subscription, destination) => {
        subscription.unsubscribe();
      });
      this.subscriptions.clear();

      // 断开连接
      this.client.deactivate();
      this.connected = false;
      this.reconnectAttempts = 0;
    }
  }

  // 发送用户状态变更消息
  sendUserStatus(status: boolean) {
    if (this.client && this.connected) {
      const message = {
        status: status,
        timestamp: Date.now()
      };
      
      log.debug('发送用户状态消息:', message);
      this.client.publish({
        destination: '/app/user/status',
        body: JSON.stringify(message)
      });
    } else {
      log.error('WebSocket未连接，无法发送用户状态消息');
    }
  }

  // 获取当前用户信息
  getUserInfo() {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/user/info',
        body: JSON.stringify({})
      });
    } else {
      log.error('WebSocket未连接，无法发送消息');
    }
  }
  
  // 发送聊天消息
  sendMessage(message: any) {
    if (this.client && this.connected) {
      this.client.publish({
        destination: '/app/chat',
        body: JSON.stringify(message)
      });
    } else {
      log.error('WebSocket未连接，无法发送聊天消息');
    }
  }

  // 检查连接状态
  isConnected(): boolean {
    log.debug('检查WebSocket连接状态:', this.connected);
    return this.connected;
  }
}

// 创建单例实例
const webSocketService = new WebSocketService();

export default webSocketService;