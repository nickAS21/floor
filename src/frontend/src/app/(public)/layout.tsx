"use client";

import { Container, useMantineColorScheme } from "@mantine/core";
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
        <Container>{children}</Container>
    )
}