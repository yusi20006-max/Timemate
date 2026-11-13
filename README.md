<div align="center">
  <img src="assets/timemate-banner.svg" alt="TimeMate — Wake Up Smarter" width="100%" />
  <br/>

  **دستیار هوشمند بیدارباش و مدیریت عادت‌های روزانه**

  *A smarter way to wake up and organize your day.*

  <p>
    <img src="https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white" alt="Android" />
    <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin" />
    <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
    <img src="https://img.shields.io/badge/Calendar-Persian%20(Jalali)-00BFA6" alt="Persian calendar" />
    <a href="https://github.com/yusi20006-max/Timemate/actions"><img src="https://img.shields.io/github/actions/workflow/status/yusi20006-max/Timemate/build-apk.yml?branch=main&label=CI&logo=github" alt="CI status" /></a>
  </p>
</div>

---

## فارسی

**TimeMate** یک اپلیکیشن اندرویدی برای بیدارباش هوشمند، مدیریت یادآورها، پیگیری عادت‌ها و کار با تقویم شمسی است. رابط کاربری با Kotlin و Jetpack Compose ساخته شده و اطلاعات اصلی برنامه به‌صورت محلی روی دستگاه نگهداری می‌شوند.

<div align="center">
  <img src="assets/timemate-icon.svg" alt="نشان برنامه TimeMate" width="116" />
</div>

### قابلیت‌ها

| قابلیت | توضیح |
|---|---|
| ⏰ بیدارباش هوشمند | زمان‌بندی زنگ، نمایش تمام‌صفحه هنگام زنگ و بازیابی زمان‌بندی پس از راه‌اندازی مجدد دستگاه |
| 🔁 چرت قابل تنظیم | انتخاب فاصله Snooze، حفظ وضعیت جلسه و هشدار پس از چند بار چرت |
| 📊 تاریخچه و آمار | ثبت جلسات بیدارباش، تعداد چرت‌ها و نمایش روندهای آماری |
| 🗓️ تقویم شمسی | محاسبات تاریخ جلالی به‌صورت آفلاین |
| ✅ یادآورها و عادت‌ها | مدیریت کارها، اولویت‌بندی و پیگیری زنجیره عادت‌ها |
| 🎨 سه تم رنگی | انتخاب تم و ذخیره دائمی آن با DataStore |
| 💾 پشتیبان‌گیری | خروجی پشتیبان محلی JSON برای داده‌های پشتیبانی‌شده |

### سازگاری و راه‌اندازی اولیه

در اولین اجرا، راهنمای مجوزها و تنظیمات پس‌زمینه متناسب با سازنده گوشی نمایش داده می‌شود. راهنمای Xiaomi/Redmi/POCO، Samsung و سایر دستگاه‌های Android مسیرهای متفاوتی دارند تا احتمال محدودشدن اجرای زنگ کاهش یابد.

> نام گزینه‌ها و مسیر تنظیمات ممکن است با نسخه Android و رابط سازنده تغییر کند. برای عملکرد قابل‌اعتماد زنگ، مجوزهای درخواستی برنامه را بررسی کنید.

### دریافت و نصب

1. وارد بخش [Releases](https://github.com/yusi20006-max/Timemate/releases) شوید؛ اگر نسخه‌ای منتشر نشده، از بخش [Actions](https://github.com/yusi20006-max/Timemate/actions) آخرین اجرای موفق را باز کنید.
2. فایل APK را از بخش **Artifacts** دانلود و از ZIP استخراج کنید.
3. APK را روی دستگاه Android نصب کنید.

**توجه:** خروجی Release فعلی در CI بدون کلید اختصاصی ناشر ساخته می‌شود و برای انتشار عمومی نهایی باید با کلید معتبر امضا شود.

### ساخت پروژه

پروژه را در Android Studio باز کنید و از مسیر زیر APK بسازید:

`Build > Build Bundle(s) / APK(s) > Build APK(s)`

ساخت خودکار Debug و Release APK نیز از طریق GitHub Actions پیکربندی شده است.

---

## English

**TimeMate** is an Android app for smarter alarms, reminders, habit tracking, and the Persian (Jalali) calendar. It is built with Kotlin and Jetpack Compose, with core app data stored locally on the device.

### Highlights

- **Smart alarms:** scheduled alarms, a full-screen ringing experience, and reboot recovery.
- **Configurable snooze:** choose snooze intervals and keep session state.
- **Alarm history & statistics:** review completed alarm sessions and usage trends.
- **Offline Jalali calendar:** Persian date calculations without a network dependency.
- **Reminders & habits:** organize tasks, set priorities, and track daily streaks.
- **Three persistent themes:** switch between color palettes, saved with DataStore.
- **Local backup:** JSON export for supported app data.

### Build

Open the project in Android Studio and select:

`Build > Build Bundle(s) / APK(s) > Build APK(s)`

GitHub Actions also builds Debug and Release APK artifacts. The current Release artifact is unsigned with a project-specific signing key and must be signed before public distribution.

---

## فناوری‌ها | Tech Stack

- Kotlin
- Jetpack Compose
- Material 3
- Room
- Preferences DataStore
- AlarmManager
- GitHub Actions

## مشارکت | Contributing

برای گزارش خطا یا پیشنهاد قابلیت، از بخش [Issues](https://github.com/yusi20006-max/Timemate/issues) استفاده کنید. لطفاً مراحل بازتولید خطا، نسخه Android و مدل دستگاه را بنویسید.

For bug reports and feature requests, please open a [GitHub Issue](https://github.com/yusi20006-max/Timemate/issues) and include reproduction steps, Android version, and device model.

---

<div align="center">

**TimeMate — Wake Up Smarter.**

ساخته‌شده برای شروع بهتر هر روز.

</div>
