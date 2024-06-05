import { createSlice } from "@reduxjs/toolkit";

type TokenObject = {
    accessToken: string;
    expiresTimeInSec: number;
    refreshToken: string;
    tokenType: string;
};

type AuthState = {
    user: {
        username: string
        token: TokenObject;
    } | null;
}

const initialState: AuthState = {
    user: null
}

type UserPayload = {
    username: string;
    token: TokenObject;
}

export const authSlice = createSlice({
    name: "auth",
    initialState,
    reducers: {
        clearUser: (state) => {
            state.user = null;
        },
        setUser: (state, { payload }: { payload: UserPayload }) => {
            state.user = payload;
        },
        refreshToken: (state, { payload }: { payload: TokenObject }) => {
            state.user && (state.user.token = payload);
        }
    }
});

export const { clearUser, setUser, refreshToken } = authSlice.actions;
export const authReducer = authSlice.reducer;