
package com.epichust.cxf.ws.sap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>deliveryDetailDataTO complex type�� Java �ࡣ
 * 
 * <p>����ģʽƬ��ָ�������ڴ����е�Ԥ�����ݡ�
 * 
 * <pre>
 * &lt;complexType name="deliveryDetailDataTO"&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="deliveryDate" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="deliveryPlan" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="deliveryPlanNum" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="lineItemTypeCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="lotCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="mrlCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="mrlGrade" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="orderCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="orderRowNum" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="planPriQty" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="rowNum" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *         &lt;element name="taskCode" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/&gt;
 *       &lt;/sequence&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "deliveryDetailDataTO", propOrder = {
    "deliveryDate",
    "deliveryPlan",
    "deliveryPlanNum",
    "lineItemTypeCode",
    "lotCode",
    "mrlCode",
    "mrlGrade",
    "orderCode",
    "orderRowNum",
    "planPriQty",
    "rowNum",
    "taskCode"
})
public class DeliveryDetailDataTO {

    protected String deliveryDate;
    protected String deliveryPlan;
    protected String deliveryPlanNum;
    protected String lineItemTypeCode;
    protected String lotCode;
    protected String mrlCode;
    protected String mrlGrade;
    protected String orderCode;
    protected String orderRowNum;
    protected String planPriQty;
    protected String rowNum;
    protected String taskCode;

    /**
     * ��ȡdeliveryDate���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDeliveryDate() {
        return deliveryDate;
    }

    /**
     * ����deliveryDate���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDeliveryDate(String value) {
        this.deliveryDate = value;
    }

    /**
     * ��ȡdeliveryPlan���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDeliveryPlan() {
        return deliveryPlan;
    }

    /**
     * ����deliveryPlan���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDeliveryPlan(String value) {
        this.deliveryPlan = value;
    }

    /**
     * ��ȡdeliveryPlanNum���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getDeliveryPlanNum() {
        return deliveryPlanNum;
    }

    /**
     * ����deliveryPlanNum���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setDeliveryPlanNum(String value) {
        this.deliveryPlanNum = value;
    }

    /**
     * ��ȡlineItemTypeCode���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLineItemTypeCode() {
        return lineItemTypeCode;
    }

    /**
     * ����lineItemTypeCode���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLineItemTypeCode(String value) {
        this.lineItemTypeCode = value;
    }

    /**
     * ��ȡlotCode���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLotCode() {
        return lotCode;
    }

    /**
     * ����lotCode���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLotCode(String value) {
        this.lotCode = value;
    }

    /**
     * ��ȡmrlCode���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMrlCode() {
        return mrlCode;
    }

    /**
     * ����mrlCode���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMrlCode(String value) {
        this.mrlCode = value;
    }

    /**
     * ��ȡmrlGrade���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getMrlGrade() {
        return mrlGrade;
    }

    /**
     * ����mrlGrade���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setMrlGrade(String value) {
        this.mrlGrade = value;
    }

    /**
     * ��ȡorderCode���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrderCode() {
        return orderCode;
    }

    /**
     * ����orderCode���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrderCode(String value) {
        this.orderCode = value;
    }

    /**
     * ��ȡorderRowNum���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getOrderRowNum() {
        return orderRowNum;
    }

    /**
     * ����orderRowNum���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setOrderRowNum(String value) {
        this.orderRowNum = value;
    }

    /**
     * ��ȡplanPriQty���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPlanPriQty() {
        return planPriQty;
    }

    /**
     * ����planPriQty���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPlanPriQty(String value) {
        this.planPriQty = value;
    }

    /**
     * ��ȡrowNum���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getRowNum() {
        return rowNum;
    }

    /**
     * ����rowNum���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setRowNum(String value) {
        this.rowNum = value;
    }

    /**
     * ��ȡtaskCode���Ե�ֵ��
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getTaskCode() {
        return taskCode;
    }

    /**
     * ����taskCode���Ե�ֵ��
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setTaskCode(String value) {
        this.taskCode = value;
    }

}
