"use client";

import Header from "@/modules/Header";
import Sidebar from "@/modules/Sidebar";
import { Box, useMantineColorScheme } from "@mantine/core";
import { useLayoutEffect } from "react";

type Props = {
    children: React.ReactNode
}

export default function Layout({ children }: Props) {
    const { colorScheme } = useMantineColorScheme();

    // It's used to sync Mantine's color scheaming with Tailwind's dark selector
    useLayoutEffect(() => {
        if (colorScheme === "dark") {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
        }
    }, [colorScheme])

    return (
        <Box className="dashboard_layout dark:text-white">
            <Sidebar />
            <Header />
            <div className="h-[calc(100vh - 70px)] board py-2 px-6">
                {children}
            </div>
        </Box>
    )
}