"use client";

import { NotifyProps } from "@/components/Notify";
import { createContext, Dispatch, SetStateAction, useState } from "react";
import { useNotification } from "../hooks/useNotification";

type ContextType = {
  props: Omit<NotifyProps, "onClose">;
  clear: () => void;
  setNotification: (type: "Success" | "Failed", text?: string) => void;
  setParams: Dispatch<SetStateAction<NotifyFnParams>>;
};

export const notifyContext = createContext<ContextType>({} as ContextType);

type NotifyProviderProps = {
  children: React.ReactNode;
};

type NotifyFnParams = {
  success: Omit<NotifyProps, "onClose" | "kind" | "visible">;
  failed: Omit<NotifyProps, "onClose" | "kind" | "visible">;
};

export const NotifyProvider = ({ children }: NotifyProviderProps) => {
  const [params, setParams] = useState<NotifyFnParams>({
    failed: {
      text: "Failed!",
      color: "red",
    },
    success: {
      text: "Success!",
      color: "green",
    },
  });

  const [setNotification, { props, clear }] = useNotification(params);

  return (
    <notifyContext.Provider
      value={{ props, clear, setNotification, setParams }}
    >
      {children}
    </notifyContext.Provider>
  );
};
