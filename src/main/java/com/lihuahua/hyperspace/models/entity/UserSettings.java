package com.lihuahua.hyperspace.models.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_settings")
public class UserSettings {
    
    @TableId("user_id")
    private String userId;
    
    @TableField("dark_mode")
    private Boolean darkMode;
    
    @TableField("background_image")
    private String backgroundImage;
    
    @TableField("background_opacity")
    private Integer backgroundOpacity;
    
    @TableField("layout")
    private String layout;
    
    @TableField("personal_signature")
    private String personalSignature;
    
    @TableField("gender")
    private String gender;
    
    @TableField("age")
    private Integer age;
    
    @TableField("created_at")
    private Date createdAt;
    
    @TableField("updated_at")
    private Date updatedAt;
    
    public Date getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    
    public Date getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getPersonalSignature() {
        return personalSignature;
    }
    
    public void setPersonalSignature(String personalSignature) {
        // 限制个人签名长度为200字符
        if (personalSignature != null && personalSignature.length() > 200) {
            this.personalSignature = personalSignature.substring(0, 200);
        } else {
            this.personalSignature = personalSignature;
        }
    }
    
    public String getGender() {
        return gender;
    }
    
    public void setGender(String gender) {
        this.gender = gender;
    }
    
    public Integer getAge() {
        return age;
    }
    
    public void setAge(Integer age) {
        this.age = age;
    }
}