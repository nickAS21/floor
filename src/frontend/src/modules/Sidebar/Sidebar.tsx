"use client";

import Link from "next/link";
import { IconLayoutDashboard, IconLogout } from "@tabler/icons-react";
import { UnstyledButton } from "@mantine/core";

import { useAppDispatch } from "@/shared/redux/store";
import { clearUser } from "@/shared/redux/authReducer";

export default function Sidebar() {
    const dispatch = useAppDispatch();

    return (
        <section className="flex flex-col h-screen toolbar border-r-2 border-r-[#343434]">
            <Link href="/">Logo</Link>
            <div className="flex flex-col">
                <IconLayoutDashboard />
            </div>
            <UnstyledButton className='cursor-pointer mt-auto' onClick={() => dispatch(clearUser())}>
                <IconLogout className="dark:text-red-500"/>
            </UnstyledButton>
        </section>
    );
}