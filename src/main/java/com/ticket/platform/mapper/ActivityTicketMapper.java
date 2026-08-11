package com.ticket.platform.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ticket.platform.entity.ActivityTicket;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * 库存操作：
 *  deductStock   乐观锁扣减，条件 stock >= count 保证不超卖
 *  incrementStock 回补库存
 */
public interface ActivityTicketMapper extends BaseMapper<ActivityTicket> {

    @Update("UPDATE `activity_ticket` SET stock = stock - #{count} " +
            "WHERE id = #{id} AND stock >= #{count}")
    int deductStock(@Param("id") Long id, @Param("count") int count);

    @Update("UPDATE `activity_ticket` SET stock = stock + #{count} " +
            "WHERE id = #{id}")
    int incrementStock(@Param("id") Long id, @Param("count") int count);
}
