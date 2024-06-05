"use client";

import { Box, useMantineColorScheme } from "@mantine/core";
import { useContext, useLayoutEffect } from "react";

import Notify from "@/components/Notify";
import Header from "@/modules/Header";
import Sidebar from "@/modules/Sidebar";
import { notifyContext } from "@/shared/context/notifications";

type Props = {
    children: React.ReactNode;
}

export default function Layout({ children }: Props) {
    const { colorScheme } = useMantineColorScheme();
    const { props, clear } = useContext(notifyContext);

    // It's used to sync Mantine's color scheaming with Tailwind's dark selector
    useLayoutEffect(() => {
        if (colorScheme === "dark") {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
        }
    }, [colorScheme])

    return (
        <>
            <Box className="dashboard_layout dark:text-white">
                <Sidebar />
                <Header />
                <div className="h-[calc(100vh - 70px)] board py-2 px-6">
                    {children}
                </div>
            </Box>
            <Notify {...props} onClose={clear} />
        </>
    )
}