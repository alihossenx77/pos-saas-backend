package com.pos.saas.security;

/**
 * একটি SaaS Application-এ একাধিক কোম্পানি বা দোকান
 * একই Application ব্যবহার করতে পারে।
 *
 * প্রত্যেক Tenant-এর Data আলাদা রাখার জন্য এই ক্লাস
 * বর্তমান Request-এর Tenant ID সংরক্ষণ করে।
 *
 * JWT Token থেকে Tenant ID নিয়ে এখানে সেট করা হয়।
 * পরে Service Layer থেকে এই ID ব্যবহার করে , নির্দিষ্ট Tenant-এর Data Access করা হয়।
 */
public final class TenantContext {

    //  প্রতিটি Thread-এর জন্য আলাদা Tenant ID রাখা।
    // ফলে একাধিক User একসাথে Request পাঠালেও
    // তাদের Tenant ID একে অপরের সাথে মিশে যাবে না।
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    //  এই Utility Class-এর Object তৈরি বন্ধ রাখা।
    private TenantContext() {}

    //  বর্তমান Request-এর Tenant ID সংরক্ষণ করা।
    // JWT থেকে পাওয়া Tenant ID এখানে সেট করা হয়।
    public static void setTenantId(Long id) {
        CURRENT.set(id);
    }

    // Purpose: বর্তমানে Login করা User-এর Tenant ID পাওয়া।
    // Service Layer এই ID ব্যবহার করে Tenant অনুযায়ী Data খুঁজে নেয়।
    public static Long getTenantId() {
        return CURRENT.get();
    }

    // Purpose: Request শেষ হলে সংরক্ষিত Tenant ID মুছে ফেলা।
    // কারণ Spring Boot একই Thread পরবর্তী Request-এ ব্যবহার করতে পারে।
    // তাই পুরোনো Tenant ID থেকে Data Leak হওয়া প্রতিরোধ করে।
    public static void clear() {
        CURRENT.remove();
    }
}