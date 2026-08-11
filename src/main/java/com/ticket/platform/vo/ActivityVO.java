package com.ticket.platform.vo;

import com.ticket.platform.entity.Activity;
import com.ticket.platform.entity.ActivityTicket;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 活动详情视图对象：活动信息 + 票种列表（缓存里存的就是它）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActivityVO {

    private Activity activity;

    private List<ActivityTicket> tickets;
}
