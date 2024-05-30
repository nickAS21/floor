import { useAppSelector } from "../redux/store"

export const useUser = () => {
    const { user } = useAppSelector(state => state.auth);
    // Convert any value to boolean
    const isAuth = !!user;

    return { user, isAuth }
}