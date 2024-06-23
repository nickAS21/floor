"use client";

import { Box, useMantineColorScheme } from "@mantine/core";
import { useContext, useEffect, useLayoutEffect } from "react";

import Notify from "@/components/Notify";
import Header from "@/modules/Header";
import Sidebar from "@/modules/Sidebar";
import { notifyContext } from "@/shared/context/notifications";
import { useUser } from "@/shared/hooks/useUser";
import { useRouter } from "next/navigation";
import {
  IconAlertTriangleFilled,
  IconCircleCheckFilled,
} from "@tabler/icons-react";

type Props = {
  children: React.ReactNode;
};

export default function Layout({ children }: Props) {
  const { colorScheme } = useMantineColorScheme();
  const { props, clear, setParams } = useContext(notifyContext);

  const { isAuth } = useUser();
  const router = useRouter();

  // It's used to sync Mantine's color scheaming with Tailwind's dark selector
  useLayoutEffect(() => {
    if (colorScheme === "dark") {
      document.documentElement.classList.add("dark");
    } else {
      document.documentElement.classList.remove("dark");
    }
  }, [colorScheme]);

  useEffect(() => {
    if (!isAuth) router.replace("/login");
  }, [isAuth]);

  useEffect(() => {
    setParams({
      failed: {
        text: "Failed!",
        icon: (
          <IconAlertTriangleFilled className="text-red-500 dark:text-orange-500" />
        ),
        classNames: {
          icon: "bg-transparent",
          closeButton: "text-black dark:text-white",
          description: "text-red-500 dark:text-orange-500",
          progress: "bg-red-500 dark:bg-orange-500",
        },
      },
      success: {
        text: "Success!",
        icon: <IconCircleCheckFilled className="text-green-500" />,
        classNames: {
          icon: "bg-transparent",
          closeButton: "text-black dark:text-white",
          description: "text-green-500",
          progress: "bg-green-500",
        },
      },
    });
  }, []);

  return (
    <>
      <Box className="dashboard_layout dark:text-white">
        <Sidebar />
        <Header />
        <div className="h-[calc(100vh - 70px)] board py-2 px-6">{children}</div>
      </Box>
      <Notify {...props} onClose={clear} />
    </>
  );
}
