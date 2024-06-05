"use client";

import { useRouter } from "next/navigation";
import { Button, TextInput } from "@mantine/core";
import { useForm, isNotEmpty } from "@mantine/form";
import { useContext, useEffect } from "react";
import { AxiosError, isAxiosError } from "axios";

import classes from "./classes.module.css";
import { cn } from "@/shared/lib/utils";
import { useAppDispatch } from "@/shared/redux/store";
import { setUser } from "@/shared/redux/authReducer";
import { useUser } from "@/shared/hooks/useUser";
import { fetcher } from "@/shared/lib/utils";
import Notify from "@/components/Notify";
import { notifyContext } from "@/shared/context/notifications";
import { IconAlertTriangleFilled, } from "@tabler/icons-react";


export default function LoginForm() {
    const { setNotification, props, clear, setParams } = useContext(notifyContext);
    const router = useRouter();
    const dispatch = useAppDispatch();
    const { isAuth } = useUser();

    useEffect(() => {
        // Customize notification
        setParams({
            failed: {
                text: 'Login failed!',
                icon: <IconAlertTriangleFilled size={24} fill="#DC362E" />,
                color: 'transparent',
              },
              success: {
                text: 'Omitted.'
              }
        })
    }, []);

    const form = useForm({
        initialValues: {
            username: '',
            password: '',
            apiEnabled: false,
        },
        validate: {
            username: isNotEmpty("Username is required."),
            password: isNotEmpty("Password is required."),
        }
    });

    // When the user attemps to log in while is already authorized, redirect to the main page 
    useEffect(() => {
        if (isAuth) router.replace("/");
    }, []);

    const handleSubmit = async (values: typeof form["values"]) => {
        try {
            const res = await fetcher({
                url: "/api/auth/login",
                method: "post",
                data: {
                    username: values.username,
                    password: values.password
                }
            });

            dispatch(setUser({ username: values.username, token: res.data.token }));
            router.push("/");
        } catch (err) {
            if (isAxiosError(err)) {
                console.log(err);
                setNotification("Failed", err.message);
            }
        }
    }

    return <>
        <form onSubmit={form.onSubmit(handleSubmit)} className="flex flex-col items-center justify-center h-screen">
            <div className={cn("p-4 rounded-md max-w-[414px] w-full dark:text-white", classes.boxShadow)}>
                <hgroup className="flex flex-col items-center mb-4">
                    <h1 className="text-xl font-semibold">Smart Tuya and Solarman Control</h1>
                    <p className="text-center text-sm text-gray-700 dark:text-orange-400">Secure Login to Manage Your Smart Home & Solar Energy</p>
                </hgroup>
                <TextInput label="Username" {...form.getInputProps("username")} classNames={{
                    root: "form-root",
                    label: 'form-label',
                    input: cn(
                        "form-input dark:border-[darkgray] dark:text-white",
                        form?.errors?.username && 'form-error--input'
                    ),
                    error: 'form-error',
                }} />
                <TextInput label="Password" {...form.getInputProps("password")} classNames={{
                    root: "form-root mt-5",
                    label: 'form-label',
                    input: cn(
                        "form-input dark:border-[darkgray] dark:text-white",
                        form?.errors?.password && 'form-error--input'
                    ),
                    error: 'form-error',
                }} />
                <Button fullWidth type="submit" variant="default" classNames={{ root: "mt-6 bg-[#b6ff6b] dark:bg-orange-400 transition hover:bg-[#b1ff62] text-black hover:text-white rounded-full" }}>Log in</Button>
            </div>
        </form>
        <Notify {...props} onClose={clear} />
    </>
}