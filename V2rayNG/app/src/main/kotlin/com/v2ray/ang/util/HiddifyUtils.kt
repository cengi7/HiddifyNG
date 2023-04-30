package com.v2ray.ang.util

import android.content.Context
import android.net.Uri
import android.text.SpannableString
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.v2ray.ang.R
import com.v2ray.ang.dto.SubscriptionItem
import com.v2ray.ang.extension.*
import com.v2ray.ang.util.Utils.getLocale
import java.util.Locale

class HiddifyUtils {
    companion object {
        private val subStorage by lazy {
            MMKV.mmkvWithID(
                MmkvManager.ID_SUB,
                MMKV.MULTI_PROCESS_MODE
            )
        }

        fun toTotalUsedGig(totalInBytes: Long, usedInBytes: Long, context: Context): String {
            val total = totalInBytes.toDouble() / 1073741824
            val used = usedInBytes.toDouble() / 1073741824
            return if (getLocale(context) == Locale("fa"))
                String.format("%.0f/%.0f G", used, total).toPersianDigit()
            else
                String.format("%.0f/%.0f G", used, total)
        }

        fun timeToRelativeDate(
            time: Long,
            totalInBytes: Long,
            usedInBytes: Long,
            context: Context
        ): SpannableString {
            if (time < 0)
                return "".bold("")

            val now = System.currentTimeMillis() / 1000
            val diffInMillis = (time - now) / 86400
            if (diffInMillis <= 0)
                return if (getLocale(context) == Locale("fa") || getLocale(context).toString() == "fa_IR")
                    "منقضی شده".bold("")
                else "Expired".bold("")

            if (totalInBytes == usedInBytes)
                return if (getLocale(context) == Locale("fa") || getLocale(context).toString() == "fa_IR")
                    "اتمام حجم".bold("")
                else "Completion of the volume".bold("")


            return if (getLocale(context) == Locale("fa") || getLocale(context).toString() == "fa_IR") {
                if (diffInMillis > 10)
                    "$diffInMillis روز \n باقیمانده".toPersianDigit()
                        .colorlessTextPart("باقیمانده", context.getColorEx(R.color.colorBorder))
                else
                    "$diffInMillis روز \n باقیمانده".toPersianDigit()
                        .colorlessTextPart("باقیمانده", context.getColorEx(R.color.colorBorder))
                        .colorlessTextPart(
                            "${diffInMillis.toString().toPersianDigit()} روز ",
                            context.getColorEx(R.color.colorRed)
                        )
            } else {
                if (diffInMillis > 10)
                    "$diffInMillis days \n Remain".colorlessTextPart(
                        "Remain",
                        context.getColorEx(R.color.colorBorder)
                    )
                else
                    "$diffInMillis days \n Remain".colorlessTextPart(
                        "Remain",
                        context.getColorEx(R.color.colorBorder)
                    ).colorlessTextPart("$diffInMillis days ", context.getColorEx(R.color.colorRed))
            }
        }

        fun checkState(time: Long, totalInBytes: Long, usedInBytes: Long): String {
            if (time < 0)
                return "disable"

            val now = System.currentTimeMillis() / 1000
            val diffInMillis = (time - now) / 86400
            if (diffInMillis <= 0)
                return "disable"

            if (totalInBytes == usedInBytes)
                return "disable"

            return "enable"
        }

        fun extract_package_info_from_response(response: Utils.Response?, subid: String) {
            if (!subid.isNullOrEmpty() && response?.headers != null) {
                val json = subStorage?.decodeString(subid)
                if (!json.isNullOrBlank()) {
                    var sub = Gson().fromJson(json, SubscriptionItem::class.java)
                    var userinfo =
                        response.headers.getOrDefault("Subscription-Userinfo", null)?.firstOrNull()
                    var homepage =
                        response.headers.getOrDefault("Profile-Web-Page-Url", null)?.firstOrNull()
                    var content_disposition =
                        response.headers.getOrDefault("Content-Disposition", null)?.firstOrNull()
                    var profile_update_interval =
                        response.headers.getOrDefault("Profile-Update-Interval", null)
                            ?.firstOrNull()
                    if (!content_disposition.isNullOrEmpty()) {
                        val regex = "filename=\"([^\"]+)\"".toRegex()
                        val matchResult = regex.find(content_disposition)
                        sub.remarks = matchResult?.groupValues?.getOrNull(1) ?: sub.remarks
                    } else if (!response.url.isNullOrEmpty()) {
                        var uri = Uri.parse(response.url)
                        sub.remarks =
                            Utils.getQueryParameterValueCaseInsensitive(uri, "Name") ?: sub.remarks
                    }
                    if (!homepage.isNullOrEmpty()) {
                        sub.home_link = homepage
                    }
                    if (!userinfo.isNullOrEmpty()) {
                        fun get(regex: String): String? {
                            return regex.toRegex().findAll(userinfo).mapNotNull {
                                if (it.groupValues.size > 1) it.groupValues[1] else null
                            }.firstOrNull();
                        }

                        sub.used = 0
                        get("upload=([0-9]+)")?.apply {
                            sub.used += toLong()
                        }
                        get("download=([0-9]+)")?.apply {
                            sub.used += toLong()
                        }
                        sub.total = get("total=([0-9]+)")?.toLong() ?: 0

                        sub.expire = get("expire=([0-9]+)")?.toLong() ?: 0
                    }

                    subStorage?.encode(subid, Gson().toJson(sub))
                }
            }
        }


    }

}