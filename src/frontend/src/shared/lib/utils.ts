import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';
import axios from "axios";

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

export const fetcher = axios.create({
  baseURL: process.env.NODE_ENV === "production" ? process.env.NEXT_PUBLIC_BACKEND_URL : process.env.NEXT_PUBLIC_DEV_BACKEND_URL,
  headers: {
    "Content-Type": "application/json"
  }
});