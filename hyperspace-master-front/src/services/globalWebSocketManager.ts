import webSocketService from './websocket';

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
    // console.log('WebSocket连接计数增加，当前计数:', this.connectionCount);
    
    // 只有在没有连接时才建立连接
    if (this.connectionCount === 1) {
      // console.log('建立新的WebSocket连接');
      webSocketService.connect(
        this.userStatusCallback || this.defaultUserStatusCallback, 
        this.messageCallback || this.defaultMessageCallback
      );
    }
  }

  // 减少连接引用计数
  decrementConnection(): void {
    this.connectionCount--;
    // console.log('WebSocket连接计数减少，当前计数:', this.connectionCount);
    
    // 不要立即断开连接，让连接保持活跃
    // 只有在确定不需要时才断开连接
    /*
    if (this.connectionCount <= 0) {
      // 最后一个引用，断开连接
      this.connectionCount = 0;
      webSocketService.disconnect();
    }
    */
  }

  // 默认用户状态回调函数
  private defaultUserStatusCallback = (data: any) => {
    // console.log('默认用户状态回调:', data);
    // 通过全局事件发送
    window.dispatchEvent(new CustomEvent('userStatusChange', { detail: data }));
  };

  // 默认消息回调函数
  private defaultMessageCallback = (data: any) => {
    // console.log('默认消息回调:', data);
    // 通过全局事件发送
    window.dispatchEvent(new CustomEvent('realTimeMessage', { detail: data }));
  };

  // 设置用户状态回调
  setUserStatusCallback(callback: (data: any) => void): void {
    // console.log('设置用户状态回调函数');
    this.userStatusCallback = callback;
    // 如果已经连接，更新回调但不重新连接
    if (this.connectionCount > 0) {
      // console.log('WebSocket已连接，更新用户状态回调函数');
      webSocketService.updateUserStatusCallback(this.userStatusCallback);
    }
  }

  // 设置消息回调
  setMessageCallback(callback: (data: any) => void): void {
    // console.log('设置消息回调函数');
    this.messageCallback = callback;
    // 如果已经连接，更新回调但不重新连接
    if (this.connectionCount > 0) {
      // console.log('WebSocket已连接，更新消息回调函数');
      webSocketService.updateMessageCallback(this.messageCallback);
    }
  }

  // 获取连接状态
  isConnected(): boolean {
    const connected = webSocketService.isConnected();
    // console.log('检查WebSocket连接状态:', connected);
    return connected;
  }

  // 发送用户状态
  sendUserStatus(status: boolean): void {
    // console.log('发送用户状态:', status);
    webSocketService.sendUserStatus(status);
  }

  // 发送消息
  sendMessage(message: any): void {
    webSocketService.sendMessage(message);
  }
  
  // 获取当前连接计数
  getConnectionCount(): number {
    return this.connectionCount;
  }
}

export default GlobalWebSocketManager.getInstance();