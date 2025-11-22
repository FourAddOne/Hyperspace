// 用户信息管理工具类

/**
 * 用户基本信息接口，基于后端UserBasicVO类
 */
export interface UserInfo {
  userId: string;
  userName: string;
  email: string;
  avatarUrl: string;
  Ip: string;
  accessToken?: string;
  refreshToken?: string;
}

/**
 * 用户设置接口，基于后端UserSettingsVO类
 */
export interface UserSettings {
  userId: string;
  darkMode?: boolean;
  backgroundImage?: string;
  backgroundOpacity?: number;
  layout?: string;
  personalSignature?: string;
  gender?: string;
  age?: number;
}

/**
 * 保存用户信息到localStorage
 * @param userInfo 用户信息对象
 */
export const saveUserInfo = (userInfo: UserInfo): void => {
  // 移除敏感信息后再保存
  const { accessToken, refreshToken, ...userInfoToSave } = userInfo;
  localStorage.setItem('userInfo', JSON.stringify(userInfoToSave));
};

/**
 * 从localStorage获取用户信息
 * @returns 用户信息对象或null
 */
export const getUserInfo = (): UserInfo | null => {
  const userInfoStr = localStorage.getItem('userInfo');
  if (userInfoStr) {
    try {
      const parsed = JSON.parse(userInfoStr);
      // 检查是否是{code, msg, data}格式的响应
      if (parsed.data) {
        return parsed.data;
      }
      return parsed;
    } catch (e) {
      console.error('解析用户信息失败:', e);
      return null;
    }
  }
  return null;
};

/**
 * 从localStorage移除用户信息
 */
export const removeUserInfo = (): void => {
  localStorage.removeItem('userInfo');
};

/**
 * 更新用户信息的部分字段
 * @param updates 需要更新的字段
 */
export const updateUserInfo = (updates: Partial<UserInfo>): void => {
  const currentUserInfo = getUserInfo();
  if (currentUserInfo) {
    const updatedUserInfo = { ...currentUserInfo, ...updates };
    saveUserInfo(updatedUserInfo);
  }
};

/**
 * 检查用户是否已登录
 * @returns boolean
 */
export const isUserLoggedIn = (): boolean => {
  return !!getUserInfo();
};

/**
 * 获取特定用户信息字段
 * @param field 字段名
 * @returns 字段值或undefined
 */
export const getUserInfoField = <T extends keyof UserInfo>(field: T): UserInfo[T] | undefined => {
  const userInfo = getUserInfo();
  const value = userInfo ? userInfo[field] : undefined;
  return value;
};

/**
 * 获取用户ID
 * @returns 用户ID或undefined
 */
export const getUserId = (): string | undefined => {
  return getUserInfoField('userId');
};

/**
 * 获取用户名
 * @returns 用户名或undefined
 */
export const getUserName = (): string | undefined => {
  return getUserInfoField('userName');
};

/**
 * 获取用户邮箱
 * @returns 用户邮箱或undefined
 */
export const getUserEmail = (): string | undefined => {
  return getUserInfoField('email');
};

/**
 * 获取完整的头像URL
 * @param avatarUrl 头像URL
 * @returns 完整的头像URL
 */
export const getFullAvatarUrl = (avatarUrl: string): string => {
  // 确保avatarUrl是字符串类型
  if (typeof avatarUrl !== 'string') {
    return '/src/assets/logo.svg';
  }
  
  // 如果avatarUrl是相对路径，需要拼接OSS域名
  if (avatarUrl && !avatarUrl.startsWith('http')) {
    // 拼接OSS域名前缀
    return `https://fourandone-hyperspace.oss-cn-hangzhou.aliyuncs.com/${avatarUrl}`;
  }
  
  // 如果avatarUrl是OSS路径，直接返回
  if (avatarUrl && (avatarUrl.includes('oss') || avatarUrl.includes('aliyuncs.com'))) {
    return avatarUrl;
  }
  
  // 如果avatarUrl是相对路径，需要拼接后端服务器地址
  if (avatarUrl && avatarUrl.startsWith('/uploads/')) {
    // 通过API代理访问上传的文件
    return '/api' + avatarUrl;
  }
  
  return avatarUrl || '/src/assets/logo.svg';
};