package sample.ble.sensortag.info;

/**
 * Created by steven on 10/7/13.
 */
public abstract class TiInfoService {


    protected TiInfoService() {
    }

    public abstract String getUUID();

    public abstract String getName();

    public abstract String getCharacteristicName(String uuid);
}
