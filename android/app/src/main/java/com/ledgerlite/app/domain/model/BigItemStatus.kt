package com.ledgerlite.app.domain.model

/**
 * 资产状态。active=使用中，ended=已结束（不计入当前日均/周均总成本）。
 */
enum class BigItemStatus {
    active, ended
}
