import webSocketService from './websocket';

// 简单的日志级别控制
const isDevelopment = process.env.NODE_ENV === 'development';

const log = {
  debug: (...args: any[]) => {
    // 生产环境不输出调试日志
  },
  info: (...args: any[]) => {
    // 生产环境不输出信息日志
  },
  warn: (...args: any[]) => {
    // 生产环境不输出警告日志
  },
  error: (...args: any[]) => {
    // 生产环境不输出错误日志
  }
};

class GlobalWebSocketManager {
  private static instance: GlobalWebSocketManager;
  private connectionCount: number = 0;
  private userStatusCallback: ((data: any) => void) | null = null;
  private messageCallback: ((data: any) => void) | null = null;

  private constructor() {}

  static getInstance(): GlobalWebSocketManager {
    if (!GlobalWebSocketManager.instance) {
      GlobalWebSocketManager.instance = new GlobalWebSocketManager();
    }
    return GlobalWebSocketManager.instance;
  }

  // 增加连接引用计数
  incrementConnection(): void {
    this.connectionCount++;
    log.debug('WebSocket连接计数增加，当前计数:', this.connectionCount);
    
    // 只有在没有连接时才建立连接
    if (this.connectionCount === 1) {
      log.debug('建立新的WebSocket连接');
      webSocketService.connect(
        this.userStatusCallback || this.defaultUserStatusCallback, 
        this.messageCallback || this.defaultMessageCallback
      );
    }
  }

  // 减少连接引用计数
  decrementConnection(): void {
    this.connectionCount--;
    log.debug('WebSocket连接计数减少，当前计数:', this.connectionCount);
    
    // 当引用计数为0时，断开连接
    if (this.connectionCount <= 0) {
      // 最后一个引用，断开连接
      this.connectionCount = 0;
      webSocketService.disconnect();
    }
  }

  // 默认用户状态回调函数
  private defaultUserStatusCallback = (data: any) => {
    log.debug('默认用户状态回调:', data);
    // 通过全局事件发送
    window.dispatchEvent(new CustomEvent('userStatusChange', { detail: data }));
  };

  // 默认消息回调函数
  private defaultMessageCallback = (data: any) => {
    log.debug('默认消息回调:', data);
    // 通过全局事件发送
    window.dispatchEvent(new CustomEvent('realTimeMessage', { detail: data }));
  };

  // 设置用户状态回调
  setUserStatusCallback(callback: (data: any) => void): void {
    log.debug('设置用户状态回调函数');
    this.userStatusCallback = callback;
    // 如果已经连接，更新回调但不重新连接
    if (this.connectionCount > 0) {
      log.debug('WebSocket已连接，更新用户状态回调函数');
      webSocketService.updateUserStatusCallback(this.userStatusCallback);
    }
  }

  // 设置消息回调
  setMessageCallback(callback: (data: any) => void): void {
    log.debug('设置消息回调函数');
    this.messageCallback = callback;
    // 如果已经连接，更新回调但不重新连接
    if (this.connectionCount > 0) {
      log.debug('WebSocket已连接，更新消息回调函数');
      webSocketService.updateMessageCallback(this.messageCallback);
    }
  }

  // 获取连接状态
  isConnected(): boolean {
    const connected = webSocketService.isConnected();
    log.debug('检查WebSocket连接状态:', connected);
    return connected;
  }

  // 发送用户状态
  sendUserStatus(status: boolean): void {
    log.debug('发送用户状态:', status);
    // 检查连接状态，如果未连接则尝试重新连接
    if (!this.isConnected()) {
      log.info('WebSocket未连接，尝试重新连接');
      webSocketService.connect(
        this.userStatusCallback || this.defaultUserStatusCallback,
        this.messageCallback || this.defaultMessageCallback
      );
    }
    
    webSocketService.sendUserStatus(status);
  }

  // 发送消息
  sendMessage(message: any): void {
    // 检查连接状态，如果未连接则尝试重新连接
    if (!this.isConnected()) {
      log.info('WebSocket未连接，尝试重新连接');
      webSocketService.connect(
        this.userStatusCallback || this.defaultUserStatusCallback,
        this.messageCallback || this.defaultMessageCallback
      );
    }
    
    webSocketService.sendMessage(message);
  }
  
  // 获取当前连接计数
  getConnectionCount(): number {
    return this.connectionCount;
  }
}

export default GlobalWebSocketManager.getInstance();