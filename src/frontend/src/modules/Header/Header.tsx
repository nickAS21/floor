"use client";

import { clearUser } from '@/shared/redux/authReducer';
import { useAppDispatch } from '@/shared/redux/store';
import { Switch, useMantineTheme, rem, useMantineColorScheme, UnstyledButton } from '@mantine/core';
import { IconSun, IconMoonStars, IconLogout } from '@tabler/icons-react';
import { useEffect, useState } from 'react';

export default function Header() {
    const theme = useMantineTheme();
    const { setColorScheme, colorScheme } = useMantineColorScheme();
    const [checked, setChecked] = useState(colorScheme === "dark" ? true : false);
    const dispatch = useAppDispatch();

    useEffect(() => {
        setColorScheme(checked ? "dark" : "light");
    }, [checked]);

    const sunIcon = (
        <IconSun
            style={{ width: rem(24), height: rem(20) }}
            stroke={2.5}
            color={theme.colors.yellow[5]}
        />
    );

    const moonIcon = (
        <IconMoonStars
            style={{ width: rem(24), height: rem(20) }}
            stroke={2.5}
            color={theme.colors.yellow[3]}
        />
    );

    return (
        <header className="header flex justify-between py-4">
            <h1>Tuya Dashboard</h1>
            <div className='flex items-center gap-4'>
                <Switch checked={checked} onChange={(event) => setChecked(event.currentTarget.checked)} size="lg" color="dark.5" onLabel={moonIcon} offLabel={sunIcon} />
                <UnstyledButton className='cursor-pointer' onClick={() => dispatch(clearUser())}>
                    <IconLogout />
                </UnstyledButton>
            </div>
        </header>
    );
}