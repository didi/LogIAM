package com.didichuxing.datachannel.system.metrcis.bean;

import com.didichuxing.datachannel.system.metrcis.bean.PeriodStatistics;

/**
 * 网卡信息
 * @author william.
 */
public class NetCardInfo {

    /**
     * mac地址
     */
    private String systemNetCardsBandMacAddress;

    /**
     * 设备名
     */
    private String systemNetCardsBandDevice;

    /**
     * 最大带宽（单位：byte）
     */
    private Long systemNetCardsBandWidth;

    /**
     * 每秒下行流量（单位：字节）
     */
    private PeriodStatistics<Long> systemNetCardsReceiveBytesPs;

    /**
     * 每秒上行流量（单位：字节）
     */
    private PeriodStatistics<Long> systemNetCardsSendBytesPs;

    public String getSystemNetCardsBandMacAddress() {
        return systemNetCardsBandMacAddress;
    }

    public void setSystemNetCardsBandMacAddress(String systemNetCardsBandMacAddress) {
        this.systemNetCardsBandMacAddress = systemNetCardsBandMacAddress;
    }

    public String getSystemNetCardsBandDevice() {
        return systemNetCardsBandDevice;
    }

    public void setSystemNetCardsBandDevice(String systemNetCardsBandDevice) {
        this.systemNetCardsBandDevice = systemNetCardsBandDevice;
    }

    public Long getSystemNetCardsBandWidth() {
        return systemNetCardsBandWidth;
    }

    public void setSystemNetCardsBandWidth(Long systemNetCardsBandWidth) {
        this.systemNetCardsBandWidth = systemNetCardsBandWidth;
    }

    public PeriodStatistics<Long> getSystemNetCardsReceiveBytesPs() {
        return systemNetCardsReceiveBytesPs;
    }

    public void setSystemNetCardsReceiveBytesPs(PeriodStatistics<Long> systemNetCardsReceiveBytesPs) {
        this.systemNetCardsReceiveBytesPs = systemNetCardsReceiveBytesPs;
    }

    public PeriodStatistics<Long> getSystemNetCardsSendBytesPs() {
        return systemNetCardsSendBytesPs;
    }

    public void setSystemNetCardsSendBytesPs(PeriodStatistics<Long> systemNetCardsSendBytesPs) {
        this.systemNetCardsSendBytesPs = systemNetCardsSendBytesPs;
    }
}
