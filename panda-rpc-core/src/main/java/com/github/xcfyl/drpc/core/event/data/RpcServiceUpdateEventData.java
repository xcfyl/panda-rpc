package com.github.xcfyl.drpc.core.event.data;

import com.github.xcfyl.drpc.core.registry.RegistryData;
import lombok.ToString;

import java.util.List;

/**
 * @author 西城风雨楼
 * @date create at 2023/6/22 23:26
 */
@ToString
public class RpcServiceUpdateEventData {
    private String serviceName;
    private List<RegistryData> newServiceList;

    public RpcServiceUpdateEventData() {

    }

    public RpcServiceUpdateEventData(String serviceName, List<RegistryData> newServiceList) {
        this.serviceName = serviceName;
        this.newServiceList = newServiceList;
    }

    public String getServiceName() {
        return serviceName;
    }

    public List<RegistryData> getNewServiceList() {
        return newServiceList;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public void setNewServiceList(List<RegistryData> newServiceList) {
        this.newServiceList = newServiceList;
    }
}