import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import axios, { AxiosError } from "axios";
import mem from "mem";

import { store as ReduxStore } from "../redux/store";
import { refreshToken as refreshTokenAction } from '../redux/authReducer';


export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const publicFetcher = axios.create({
  baseURL: process.env.NODE_ENV === "production" ? process.env.NEXT_PUBLIC_BACKEND_URL : process.env.NEXT_PUBLIC_DEV_BACKEND_URL,
  headers: {
    "Content-Type": "application/json"
  },
});

const refreshTokenFn = async () => {
  const store = JSON.parse(localStorage.getItem("persist:auth") || "");
  const session = JSON.parse(store.user);

  try {
    const response = await fetcher.post("/api/auth/refresh", null, {
      headers: {
        Authorization: `Bearer ${session?.token.refreshToken}`
      }
    });

    const { token } = response.data;

    ReduxStore.dispatch(refreshTokenAction(token));

    return token;
  } catch (err) {
    localStorage.removeItem("persist:auth");
  }
}

const maxAge = 10000;

export const memoizedRefreshToken = mem(refreshTokenFn, { maxAge });

export const fetcher = axios.create({
  baseURL: process.env.NODE_ENV === "production" ? process.env.NEXT_PUBLIC_BACKEND_URL : process.env.NEXT_PUBLIC_DEV_BACKEND_URL,
  headers: {
    "Content-Type": "application/json"
  },
  withCredentials: true,
});

fetcher.interceptors.response.use(
  (res) => {
    // FIXME
    if (res.data.status != null && res.data.status !== 200) { 
      const err = new AxiosError(res.data.message);
      err.status = res.data.status;
      throw err;
    };

    return res;
  },
  async (error) => {
    const config = error?.config;
    
    if (error?.response?.status === 401 && !config?.sent) {
      config.sent = true;
      
      const result = await memoizedRefreshToken();

      if (result) {
        config.headers.Authorization = `Bearer ${result}`;
      }

      // Refetch again but with an updated token
      return fetcher(config);
    }

    return Promise.reject(error);
  }
);

fetcher.interceptors.request.use(
  async (config) => {
    const store = JSON.parse(localStorage.getItem("persist:auth") || "");
    const session = JSON.parse(store.user);

    if (session) {
      config.headers.Authorization = `Bearer ${session?.token.refreshToken}`;
    }

    return config;
  },
  (err) => Promise.reject(err)
)