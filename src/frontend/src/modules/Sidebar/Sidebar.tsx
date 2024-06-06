"use client";

import Link from "next/link";
import { IconLayoutDashboard, IconLogout, IconSettings } from "@tabler/icons-react";
import { UnstyledButton } from "@mantine/core";

import { useAppDispatch } from "@/shared/redux/store";
import { clearUser } from "@/shared/redux/authReducer";
import { fetcher } from "@/shared/lib/utils";
import { useContext } from "react";
import { notifyContext } from "@/shared/context/notifications";
import { isAxiosError } from "axios";

export default function Sidebar() {
    const dispatch = useAppDispatch();
    const { setNotification } = useContext(notifyContext);

    const logOut = async () => {
        try {
            const res = await fetcher.post("/api/auth/logout");
            console.log(res);
            dispatch(clearUser());
        } catch (err) {
            if (isAxiosError(err)) {
                setNotification("Failed", err.message);
            }
        }
    };

    return (
        <section className="flex flex-col items-center p-4 h-screen toolbar border-r-2 border-r-[#e9ecef] dark:border-r-[#343434]">
            <Link href="/" className="mb-8 mt-2">Logo</Link>
            <div className="flex flex-col text-[#4a4949] dark:text-[white]">
                <Link href="/" title="Dashboard">
                    <IconLayoutDashboard className="dark:text-orange-400" />
                </Link>
            </div>
            <div className="mt-auto flex flex-col items-center gap-4">
                <Link href="/settings" title="Settings">
                    <IconSettings className="dark:text-orange-400"/>
                </Link>
                <UnstyledButton title="Log out" onClick={() => logOut()}>
                    <IconLogout className="dark:text-orange-400 -mr-1"/>
                </UnstyledButton>
            </div>
        </section>
    );
}