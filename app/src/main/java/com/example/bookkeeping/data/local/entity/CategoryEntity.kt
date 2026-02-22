package com.example.bookkeeping.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类数据表。
 *
 * 存储交易分类，如购物、消费、餐饮、交通等。
 * - [id]：分类 ID（如 "shopping", "dining"）
 * - [name]：分类显示名称（如 "购物"）
 * - [icon]：分类图标（可选，编码为 emoji 或资源名）
 * - [color]：分类颜色（十六进制 ARGB）
 * - [isDefault]：是否为预置分类（false 表示用户自定义）
 */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val icon: String? = null,
    val color: String? = null,
    val type: String = CategoryType.EXPENSE,
    val isDefault: Boolean = true,
)

object CategoryType {
    const val EXPENSE = "EXPENSE"
    const val INCOME = "INCOME"
}

/**
 * 预置分类常量。
 */
object DefaultCategories {
    val PAY_FOR = CategoryEntity(
        id = "pay_for",
        name = "代付",
        icon = "💳",
        color = "#FF90CAF9",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val SHOPPING = CategoryEntity(
        id = "shopping",
        name = "购物",
        icon = "🛍️",
        color = "#FF9C27B0",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val DINING = CategoryEntity(
        id = "dining",
        name = "餐饮",
        icon = "🍽️",
        color = "#FFFF9800",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val TRANSPORTATION = CategoryEntity(
        id = "transportation",
        name = "交通",
        icon = "🚗",
        color = "#FF2196F3",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val HOUSING = CategoryEntity(
        id = "housing",
        name = "住房",
        icon = "🏠",
        color = "#FF4CAF50",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val ENTERTAINMENT = CategoryEntity(
        id = "entertainment",
        name = "娱乐",
        icon = "🎬",
        color = "#FFF44336",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val COMMUNICATION = CategoryEntity(
        id = "communication",
        name = "通讯",
        icon = "📱",
        color = "#FF64B5F6",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val UTILITIES = CategoryEntity(
        id = "utilities",
        name = "生活费",
        icon = "🏠",
        color = "#FF4CAF50",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val BEAUTY = CategoryEntity(
        id = "beauty",
        name = "美容",
        icon = "💆",
        color = "#FFF48FB1",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val PET = CategoryEntity(
        id = "pet",
        name = "宠物",
        icon = "🐶",
        color = "#FFA1887F",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val SOCIAL = CategoryEntity(
        id = "social",
        name = "人情社交",
        icon = "🫶",
        color = "#FFBA68C8",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val FAMILY = CategoryEntity(
        id = "family",
        name = "亲子",
        icon = "🍼",
        color = "#FFFFCC80",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val REPAYMENT = CategoryEntity(
        id = "repayment",
        name = "还债",
        icon = "🧾",
        color = "#FF90A4AE",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val LEND_OUT = CategoryEntity(
        id = "lend_out",
        name = "借出",
        icon = "🤝",
        color = "#FF81D4FA",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val INVEST = CategoryEntity(
        id = "invest",
        name = "投资",
        icon = "📈",
        color = "#FFA5D6A7",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val TRAVEL = CategoryEntity(
        id = "travel",
        name = "旅行",
        icon = "🧳",
        color = "#FF4DB6AC",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val RED_PACKET = CategoryEntity(
        id = "red_packet",
        name = "红包",
        icon = "🧧",
        color = "#FFFF8A65",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val MEDICAL = CategoryEntity(
        id = "medical",
        name = "医疗",
        icon = "⚕️",
        color = "#FF2196F3",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val EDUCATION = CategoryEntity(
        id = "education",
        name = "教育",
        icon = "📚",
        color = "#FF673AB7",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val CONSUME = CategoryEntity(
        id = "consume",
        name = "消费",
        icon = "🧾",
        color = "#FFB0BEC5",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val TRANSFER = CategoryEntity(
        id = "transfer",
        name = "转账",
        icon = "🔁",
        color = "#FFCE93D8",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val OTHERS = CategoryEntity(
        id = "others",
        name = "其他",
        icon = "📌",
        color = "#FF757575",
        type = CategoryType.EXPENSE,
        isDefault = true,
    )

    val SALARY = CategoryEntity(
        id = "salary",
        name = "薪资",
        icon = "💼",
        color = "#FFFFB300",
        type = CategoryType.INCOME,
        isDefault = true,
    )

    val INVESTMENT = CategoryEntity(
        id = "investment",
        name = "理财",
        icon = "🏦",
        color = "#FF8BC34A",
        type = CategoryType.INCOME,
        isDefault = true,
    )

    val REFUND = CategoryEntity(
        id = "refund",
        name = "退款",
        icon = "↩️",
        color = "#FF4CAF50",
        type = CategoryType.INCOME,
        isDefault = true,
    )

    val INCOME_OTHERS = CategoryEntity(
        id = "income_others",
        name = "其他",
        icon = "🧩",
        color = "#FF9E9E9E",
        type = CategoryType.INCOME,
        isDefault = true,
    )

    val INCOME_DEBT = CategoryEntity(
        id = "income_debt",
        name = "收债",
        icon = "💰",
        color = "#FFFFD54F",
        type = CategoryType.INCOME,
        isDefault = true,
    )

    val BORROW_IN = CategoryEntity(
        id = "borrow_in",
        name = "借入",
        icon = "🤝",
        color = "#FFAED581",
        type = CategoryType.INCOME,
        isDefault = true,
    )

    val INCOME_RED_PACKET = CategoryEntity(
        id = "income_red_packet",
        name = "红包",
        icon = "🧧",
        color = "#FFFF8A65",
        type = CategoryType.INCOME,
        isDefault = true,
    )

    val INCOME_TRANSFER = CategoryEntity(
        id = "income_transfer",
        name = "转账",
        icon = "🔁",
        color = "#FFCE93D8",
        type = CategoryType.INCOME,
        isDefault = true,
    )

    val EXPENSE = listOf(
        PAY_FOR,
        OTHERS,
        PET,
        SOCIAL,
        FAMILY,
        BEAUTY,
        REPAYMENT,
        LEND_OUT,
        INVEST,
        TRAVEL,
        RED_PACKET,
        EDUCATION,
        MEDICAL,
        ENTERTAINMENT,
        COMMUNICATION,
        TRANSPORTATION,
        HOUSING,
        SHOPPING,
        DINING,
        CONSUME,
        TRANSFER,
        UTILITIES,
    )

    val INCOME = listOf(
        INCOME_OTHERS,
        REFUND,
        INCOME_DEBT,
        BORROW_IN,
        INCOME_RED_PACKET,
        INVESTMENT,
        SALARY,
        INCOME_TRANSFER,
    )

    val ALL = EXPENSE + INCOME
}
