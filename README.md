# 🛰️ NanoSat Finder (یابنده نانوماهواره) - v4.2

<img width="768" height="1376" alt="ساخت_بروشور_تبلیغاتی_اپلیکیشن_202606171536" src="https://github.com/user-attachments/assets/b945c3f5-fadc-49c3-a8fc-74dbc8b44246" />



[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack_Compose-M3-green.svg?logo=android)](https://developer.android.com/compose)
[![Room](https://img.shields.io/badge/Local_Database-Room_SQLite-orange.svg?logo=sqlite)](https://developer.android.com/training/data-storage/room)
[![Version](https://img.shields.io/badge/App_Version-4.2-purple.svg)](#)

A beautiful, high-performance, bilingual Android client designed for tracking, filtering, and discovering CubeSats and Nano-Satellites. Built using modern Jetpack Compose (Material Design 3), architectural components (MVVM), and local SQL data persistence with Room.

اپلیکیشن اندرویدی زیبا، پرسرعت و دو زبانه که برای ردیابی، فیلتر پیشرفته و اکتشاف نانوماهواره‌ها طراحی شده است. توسعه یافته با معماری نوین Jetpack Compose (Material 3)، الگوی MVVM و پایگاه داده محلی Room.

---

## 🌍 Key Features / ویژگی‌های کلیدی

### English
* **Bilingual Support**: Toggle instantly between English and Persian language preferences via the persistent Globe icon button in the footer.
* **Complex Multi-Criteria Filters**: Filter through CubeSat unit sizes (1U, 2U, 3U, 6U, 12U), weight ranges (Micro, Light, Medium, Heavy), operational statuses, launcher/builder country, and primary mission types.
* **Favorites (Bookmark System)**: Star your favorite satellites either directly from the main list grid or within the comprehensive technical details sheet, with database persistence.
* **Single Column Design**: Indulge in an eye-friendly, modern, single-column typography-rich feed containing Material 3 cards.
* **Modern Material 3 Theme**: Fully optimized dark cosmic aesthetic with pleasant spatial neon highlights.

### فارسی
* **پشتیبانی دو زبانه**: تغییر آنی تمام بخش‌های برنامه بین دو زبان فارسی و انگلیسی با لمس دکمه کره زمینِ در انتهای صفحه با حفظ تنظیمات در مراجعات بعدی.
* **فیلترهای پیشرفته چندگانه**: جستجو و فیلتر بر اساس اندازه بدنه (CubeSat units)، محدوده وزن (میکرو، سبک، متوسط، سنگین)، وضعیت مداری، کشور سازنده/پرتاب‌کننده و نوع ماموریت تخصصی.
* **سیستم نشان دار کردن (علاقه‌مندی‌ها)**: امکان نشان دار کردن ماهواره‌ها هم در صفحه اصلی و هم در درون صفحه جزئیات قطعی، با ذخیره‌سازی دائمی در دایکتوری محلی.
* **نمایش تک‌ستونه شیک**: چیدمان تک‌ستونه امروزی و روان کارت‌ها جهت خوانایی چشم‌نواز و دسترسی ارگونومیک به اطلاعات.
* **پوسته تاریک کیهانی**: قالب تیره مدرن با الهام از اعماق فضا به همراه رنگ‌های مکمل متمایز کننده.

---

## 🛠️ Version 4.2 Highlights / تغییرات نسخه جدید ۴.۲
* **Language Switcher**: Integrated in the right side of the footer with appropriate spacing from version text.
* **Context-Driven Navigation**: Back navigation arrows adjust orientation dynamically (Direct straight right arrow for Persian RTL and left-arrow for English LTR).
* **Reset Broom (🧹)**: Replaced circular arrow filter-clear triggers with a neat functional broom icon on the Home terminal.
* **Full Detail Favorites Toggle**: Instantly bookmark satellites while studying their extensive mission configurations and specs graphs.

---

## 📱 Screenshots / پیش‌نمایش
*(You can host screenshots or app mockups in this section)*

---

## 🏗️ Architecture & Stack / ساختار فنی و کتابخانه‌ها
- **UI Platform**: 100% Jetpack Compose (Declarative Android UI)
- **Design System**: Material Design 3
- **Dependency Management**: Gradle Kotlin DSL with Version Catalog
- **Local Database**: Room Engine for SQLite storage
- **Asynchronous Flow**: Kotlin Coroutines and StateFlow streams
- **Image Loading**: Coil Library
