import { isAxiosError } from "axios";
import { APP_PATHS } from "./constants";
import { fetcher } from "./utils";

export const getTuyaData = async () => {
  try {
    await fetcher(APP_PATHS["TUYA-DATA"]);
  } catch (err) {
    if (isAxiosError(err)) {
      throw err;
    }
  }
};

export const getSmartConfig = async () => {
  try {
    await fetcher(APP_PATHS["SMART-CONFIG"]);
  } catch (err) {
    if (isAxiosError(err)) {
      throw err;
    }
  }
};
