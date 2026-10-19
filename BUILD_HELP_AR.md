# إصلاح البناء في KlenCod IDE

1. فك ضغط المجلد `sqmotion_work` وافتح المجلد الذي يحتوي `settings.gradle` و`build.gradle` كمشروع Android/Gradle. لا تفتح مجلد `app` وحده.
2. استخدم JDK 17 إن كان خيار JDK متاحًا.
3. أول بناء يحتاج اتصالًا لتنزيل Gradle 8.7 وملفات Android Gradle Plugin 8.5.2 وSDK 34.
4. إذا بقي البناء على Building، فقد يكون تنزيل Gradle أو SDK متوقفًا.
5. هذا الإصدار يصلح إعدادات Gradle الشائعة، لكن لم يمكن تأكيد بناء APK داخل هذه البيئة لعدم توفر تنزيل Gradle من الإنترنت هنا.
