import { clsx, type ClassValue } from "clsx";
import { twMerge } from "tailwind-merge";
import axios from "axios";

import { store as ReduxStore } from "../redux/store";
import { refreshToken as refreshTokenAction } from "../redux/authReducer";
import { APP_PATHS } from "./constants";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const publicFetcher = axios.create({
  baseURL:
    process.env.NODE_ENV === "production"
      ? process.env.NEXT_PUBLIC_BACKEND_URL
      : process.env.NEXT_PUBLIC_DEV_BACKEND_URL,
  headers: {
    "Content-Type": "application/json",
  },
});

const refreshTokenFn = async () => {
  const state = ReduxStore.getState().auth.user?.token;

  try {
    const response = await fetcher.post("/api/auth/refresh", undefined, {
      headers: {
        Authorization: `Bearer ${state?.refreshToken}`,
      },
    });

    const { token } = response.data;

    ReduxStore.dispatch(refreshTokenAction(token));

    return token;
  } catch (err) {
    localStorage.removeItem("persist:auth");
    throw err;
  }
};

export const memoizedRefreshToken = refreshTokenFn;

export const fetcher = axios.create({
  baseURL:
    process.env.NODE_ENV === "production"
      ? process.env.NEXT_PUBLIC_BACKEND_URL
      : process.env.NEXT_PUBLIC_DEV_BACKEND_URL,
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: true,
});

fetcher.interceptors.response.use(
  (res) => res,
  async (error) => {
    const config = error?.config;

    if (
      // In case of log in error, we don't need to refresh the token (the same will be with registering)
      config.url !== APP_PATHS["LOGIN"] &&
      config.url !== APP_PATHS["REFRESH"] &&
      error?.response?.status === 401 &&
      !config?.sent
    ) {
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
    const state = ReduxStore.getState().auth.user?.token;

    if (state) {
      config.headers.Authorization = `Bearer ${state?.accessToken}`;
    }

    return config;
  },
  (err) => Promise.reject(err)
);
