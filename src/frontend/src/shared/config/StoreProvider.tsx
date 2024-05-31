'use client';

import React from 'react';

import { Provider } from 'react-redux';
import { persistor, store } from '../redux/store';
import { PersistGate } from 'redux-persist/integration/react';

export const StoreProvider = (props: React.PropsWithChildren) => {
    return (
        <>
            <PersistGate persistor={persistor}>
                <Provider store={store}>
                    {props.children}
                </Provider>
            </PersistGate>
        </>
    );
};