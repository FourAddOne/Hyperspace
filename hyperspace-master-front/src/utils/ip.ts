// IP地址工具函数

/**
 * 带超时控制的fetch请求
 * @param url 请求URL
 * @param options fetch选项
 * @param timeout 超时时间（毫秒）
 * @returns Promise<Response>
 */
function fetchWithTimeout(url: string, options: RequestInit = {}, timeout = 5000): Promise<Response> {
  return Promise.race([
    fetch(url, options),
    new Promise<Response>((_, reject) =>
      setTimeout(() => reject(new Error('请求超时')), timeout)
    )
  ]) as Promise<Response>;
}

/**
 * 获取用户公网IP地址
 * 通过多个第三方服务获取用户的真实公网IP地址
 * @returns Promise<string> 用户的公网IP地址
 */
export async function getUserIP(): Promise<string> {
  // 多个IP获取服务列表
  const ipServices = [
    'https://api.ipify.org?format=json',
    'https://ipapi.co/json/',
    'https://api.my-ip.io/ip.json'
  ];

  // 尝试每个服务，直到成功或全部失败
  for (const service of ipServices) {
    try {
      const response = await fetchWithTimeout(service, { 
        method: 'GET',
        mode: 'cors',
        cache: 'no-cache',
        headers: {
          'Accept': 'application/json',
        }
      }, 3000); // 3秒超时，减少等待时间
      
      if (response.ok) {
        const data = await response.json();
        // 根据不同服务返回的数据结构提取IP
        const ip = data.ip || data.query || '';
        if (ip) {
          return ip;
        }
      }
    } catch (error) {
      console.warn(`通过服务 ${service} 获取IP失败:`, error);
      // 继续尝试下一个服务
      continue;
    }
  }

  // 所有服务都失败时返回默认值
  console.error('所有IP获取服务都失败，返回默认IP');
  return '127.0.0.1';
}

/**
 * 获取客户端IP信息（包括公网IP和地理位置等）
 * @returns Promise<any> IP相关信息对象
 */
export async function getIPInfo(): Promise<any> {
  const ipServices = [
    'https://ipapi.co/json/',
    'https://api.my-ip.io/ip.json',
    'https://api.ipify.org?format=json'
  ];

  // 尝试每个服务，直到成功或全部失败
  for (const service of ipServices) {
    try {
      const response = await fetchWithTimeout(service, {
        method: 'GET',
        mode: 'cors',
        cache: 'no-cache',
        headers: {
          'Accept': 'application/json',
        }
      }, 5000); // 5秒超时
      
      if (response.ok) {
        const data = await response.json();
        if (data) {
          return data;
        }
      }
    } catch (error) {
      console.warn(`通过服务 ${service} 获取IP信息失败:`, error);
      // 继续尝试下一个服务
      continue;
    }
  }

  // 所有服务都失败时返回null
  console.error('所有IP信息获取服务都失败');
  return null;
}

export default {
  getUserIP,
  getIPInfo
};