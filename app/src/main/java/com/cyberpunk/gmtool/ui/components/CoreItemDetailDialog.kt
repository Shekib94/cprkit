package com.cyberpunk.gmtool.ui.components

import androidx.compose.runtime.Composable

/**
 * نگه‌داشته شده فقط برای سازگاری با محل‌های صدا زدن قدیمی.
 *
 * پیاده‌سازی واقعی حالا `ItemDetailSheet` است — یک نمایش‌دهنده‌ی واحد برای
 * فروشگاه، کوله، بازار شبانه، سازنده‌ی شخصیت و Agent. قبلاً هر کدام نسخه‌ی
 * خودش را داشت و راست‌چینی و چیدمانشان با هم فرق می‌کرد.
 */
@Composable
fun CoreItemDetailDialog(
    label: String,
    categoryHint: String? = null,
    onDismiss: () -> Unit
) = ItemDetailSheetByName(label = label, categoryHint = categoryHint, onDismiss = onDismiss)
