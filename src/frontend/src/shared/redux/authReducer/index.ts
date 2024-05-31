import { createSlice } from "@reduxjs/toolkit";

type AuthState = {
    user: {
        username: string
        token: string;
    } | null;
}

const initialState: AuthState = {
    user: null
}

type UserPayload = {
    username: string;
    token: string;
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
        }
    }
});

export const { clearUser, setUser } = authSlice.actions;
export const authReducer = authSlice.reducer;