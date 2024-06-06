import { useAppSelector } from "../redux/store"

export const useUser = () => {
    const user = useAppSelector(state => state.auth.user);
    // Convert any value to boolean
    const isAuth = !!user?.token;

    return { user, isAuth }
}