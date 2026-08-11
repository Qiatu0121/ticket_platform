package com.ticket.platform.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ticket.platform.common.Result;
import com.ticket.platform.entity.Activity;
import com.ticket.platform.mapper.ActivityMapper;
import com.ticket.platform.service.TicketService;
import com.ticket.platform.vo.ActivityVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/activity")
@RequiredArgsConstructor
public class ActivityController {

    private final ActivityMapper activityMapper;
    private final TicketService ticketService;

    /** 活动列表（简单场景直接查库即可） */
    @GetMapping("/list")
    public Result<List<Activity>> list() {
        return Result.ok(activityMapper.selectList(
                new LambdaQueryWrapper<Activity>().orderByDesc(Activity::getStartTime)));
    }

    /** 活动详情：走缓存 + 互斥锁防击穿 */
    @GetMapping("/{id}")
    public Result<ActivityVO> detail(@PathVariable Long id) throws Exception {
        return Result.ok(ticketService.getActivityDetail(id));
    }
}
