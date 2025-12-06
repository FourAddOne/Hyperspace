<template>
  <div class="custom-quill-editor">
    <!-- 工具栏移出 .ql-container，这样就不会一起滚动了 -->
    <div ref="toolbarElement" class="ql-toolbar" v-show="!readOnly"></div>
    <!-- .ql-container 现在只包含内容区 -->
    <div ref="editorElement" class="ql-container"></div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import apiClient from '../services/api'

const props = defineProps<{
  modelValue?: string
  placeholder?: string
  readOnly?: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: string): void
}>()

let FluentEditor: any = null
let editorInstance: any = null
const toolbarElement = ref<HTMLDivElement | null>(null)
const editorElement = ref<HTMLDivElement | null>(null)
const isEditorLoaded = ref(false)

// 工具栏按钮的title配置
const titleConfig = [
  { Choice: '.ql-bold', title: '加粗' },
  { Choice: '.ql-italic', title: '斜体' },
  { Choice: '.ql-underline', title: '下划线' },
  { Choice: '.ql-strike', title: '删除线' },
  { Choice: '.ql-blockquote', title: '引用' },
  { Choice: '.ql-code-block', title: '代码块' },
  { Choice: '.ql-header[value="1"]', title: '标题1' },
  { Choice: '.ql-header[value="2"]', title: '标题2' },
  { Choice: '.ql-list[value="ordered"]', title: '有序列表' },
  { Choice: '.ql-list[value="bullet"]', title: '无序列表' },
  { Choice: '.ql-script[value="sub"]', title: '下标' },
  { Choice: '.ql-script[value="super"]', title: '上标' },
  { Choice: '.ql-indent[value="-1"]', title: '减少缩进' },
  { Choice: '.ql-indent[value="+1"]', title: '增加缩进' },
  { Choice: '.ql-direction', title: '文本方向' },
  { Choice: '.ql-size .ql-picker-label', title: '字体大小' },
  { Choice: '.ql-header .ql-picker-label', title: '标题' },
  { Choice: '.ql-color .ql-picker-label', title: '文字颜色' },
  { Choice: '.ql-background .ql-picker-label', title: '背景色' },
  { Choice: '.ql-font .ql-picker-label', title: '字体' },
  { Choice: '.ql-align .ql-picker-label', title: '对齐方式' },
  { Choice: '.ql-link', title: '添加链接' },
  { Choice: '.ql-image', title: '插入图片' },
  { Choice: '.ql-video', title: '插入视频' },
  { Choice: '.ql-clean', title: '清除格式' },
  { Choice: '.ql-table', title: '表格' }
]

// 动态加载 Fluent Editor
const loadFluentEditor = async () => {
  if (typeof window !== 'undefined') {
    try {
      // 先导入 Quill 样式
      await import('quill/dist/quill.snow.css')
      // 再导入 Fluent Editor
      const fluentModule = await import('@opentiny/fluent-editor')
      FluentEditor = fluentModule.default
      isEditorLoaded.value = true
    } catch (error) {
      console.error('Failed to load Fluent Editor:', error)
    }
  }
}

// 图片上传处理函数
const handleImageUpload = async (file: File): Promise<string> => {
  try {
    // 创建FormData对象
    const formData = new FormData()
    formData.append('file', file)
    formData.append('fileType', 'richtext') // 指定文件类型为富文本图片
    
    // 调用后端上传接口
    const response: any = await apiClient.post('/file/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    
    if (response.code === 200 && response.data) {
      // 返回上传后的图片URL
      return response.data
    } else {
      throw new Error(response.msg || '上传失败')
    }
  } catch (error: any) {
    console.error('图片上传失败:', error)
    throw error
  }
}

// 初始化编辑器
const initEditor = async () => {
  if ((!toolbarElement.value && !props.readOnly) || !editorElement.value || !FluentEditor || editorInstance) return
  
  try {
    // 创建 Fluent Editor 实例
    if (props.readOnly) {
      editorInstance = new FluentEditor(editorElement.value, {
        modules: {
          toolbar: false,
          table: true
        },
        theme: 'snow',
        readOnly: true
      })
    } else {
      editorInstance = new FluentEditor(editorElement.value, {
        modules: {
          toolbar: [
            ['bold', 'italic', 'underline', 'strike'],
            ['blockquote', 'code-block'],
            [{ header: 1 }, { header: 2 }],
            [{ list: 'ordered' }, { list: 'bullet' }],
            [{ script: 'sub' }, { script: 'super' }],
            [{ indent: '-1' }, { indent: '+1' }],
            [{ direction: 'rtl' }],
            [{ size: ['small', false, 'large', 'huge'] }],
            [{ header: [1, 2, 3, 4, 5, 6, false] }],
            [{ color: [] }, { background: [] }],
            [{ font: [] }],
            [{ align: [] }],
            ['link', 'image', 'video'],
            ['clean'],
            ['table']
          ],
          table: true
        },
        placeholder: props.placeholder || '请输入内容...',
        theme: 'snow',
        readOnly: false
      })
      
      // 自定义图片处理逻辑
      const imageHandler = () => {
        const input = document.createElement('input')
        input.setAttribute('type', 'file')
        input.setAttribute('accept', 'image/*')
        input.click()
        
        input.onchange = async () => {
          if (input.files && input.files[0]) {
            const file = input.files[0]
            
            try {
              // 显示上传中状态
              const range = editorInstance.getSelection(true)
              editorInstance.insertEmbed(range.index, 'image', '/src/assets/loading.gif')
              
              // 上传图片
              const imageUrl = await handleImageUpload(file)
              
              // 替换为真实的图片URL
              editorInstance.deleteText(range.index, 1)
              editorInstance.insertEmbed(range.index, 'image', imageUrl)
              editorInstance.setSelection(range.index + 1)
            } catch (error: any) {
              console.error('图片上传失败:', error)
              // 删除临时的loading图片
              const range = editorInstance.getSelection(true)
              editorInstance.deleteText(range.index, 1)
              alert('图片上传失败: ' + (error.message || '未知错误'))
            }
          }
        }
      }
      
      // 绑定图片处理函数到编辑器
      editorInstance.getModule('toolbar').addHandler('image', imageHandler)
    }
    
    // 设置初始内容
    if (props.modelValue) {
      editorInstance.root.innerHTML = props.modelValue
    }
    
    // 监听文本变化
    editorInstance.on('text-change', () => {
      const content = editorInstance.root.innerHTML
      emit('update:modelValue', content)
    })
    
    // 在下一个tick添加title属性
    nextTick(() => {
      if (!props.readOnly) {
        addToolbarTitles()
      }
    })
  } catch (error) {
    console.error('Failed to initialize Fluent Editor:', error)
  }
}

// 为工具栏按钮添加title属性
const addToolbarTitles = () => {
  if (!toolbarElement.value) return
  let toolbar: Element | null = null
  
  // 直接在toolbarElement中查找
  toolbar = toolbarElement.value
  if (!toolbar) {
    setTimeout(() => {
      addToolbarTitles()
    }, 100)
    return
  }

  // 为每个工具栏项添加title
  titleConfig.forEach(item => {
    const elements = toolbar!.querySelectorAll(item.Choice)
    elements.forEach(el => {
      el.setAttribute('title', item.title)
    })
  })
}

// 监听modelValue变化
watch(() => props.modelValue, (newValue) => {
  if (editorInstance && newValue !== editorInstance.root.innerHTML) {
    editorInstance.root.innerHTML = newValue || ''
  }
})

onMounted(async () => {
  await loadFluentEditor()
  if (isEditorLoaded.value) {
    await nextTick()
    initEditor()
  }
})

onUnmounted(() => {
  if (editorInstance) {
    editorInstance = null
  }
})
</script>

<style scoped>
/* 为包裹容器设置固定高度和Flex布局 */
.custom-quill-editor {
  height: 100%; /* 继承父容器（你设置的580px）的高度 */
  display: flex;
  flex-direction: column;
}


/* 内容区容器占据剩余空间并负责滚动 */
.custom-quill-editor .ql-container {
  flex: 1; /* 关键：占据所有剩余空间 */
  overflow-y: auto; /* 只有这里滚动 */
  border: 1px solid #ccc;
  border-radius: 0 0 4px 4px;
}

/* 内容区编辑器本身不产生滚动，高度由父容器决定 */
.custom-quill-editor .ql-editor {
  height: 100%;
  min-height: 100%; /* 确保至少撑满父容器 */
  overflow-y: visible;
}
</style>