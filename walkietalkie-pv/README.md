# Walkie Talkie (Plasmo Voice) — Forge 1.20.1

ไอเท็มวิทยุสื่อสารสำหรับ Plasmo Voice
- คลิกขวาที่ไอเท็มเพื่อตั้งความถี่ 100.00 – 500.00 MHz (ละเอียด 0.01)
- ใน Plasmo Voice จะมีปุ่มใหม่ชื่อ "Walkie Talkie" (แท็บ Activations) ให้ผูกคีย์เอง
- ต้องถือวิทยุไว้ในมือ (มือหลัก/มือรอง) ถึงจะพูดได้
- คนที่มีวิทยุอยู่ในช่องเก็บของและจูนความถี่เดียวกันจะได้ยิน ไม่จำกัดระยะ (ข้ามมิติได้)
- เสียงถูกแปลงเป็นเสียงวิทยุ: ตัดแบนด์ 450-2600 Hz, เสียงแตกกรอบ, ซ่า, เสียงกรอบแกรบ
  และมี "ปี๊บ" ท้ายประโยคตอนปล่อยปุ่ม
- สูตรคราฟต์: ทองแดง/เหล็ก/เรดสโตน/ปุ่มหิน (ดู data/walkietalkie/recipes)

## วิธีได้ไฟล์ .jar (ไม่ต้องลงอะไรในเครื่อง)
1. สร้าง repo ใหม่บน GitHub แล้วอัปโหลดไฟล์ทั้งโฟลเดอร์นี้ (รวมโฟลเดอร์ .github)
2. เปิดแท็บ Actions -> Build jar -> รอ build เสร็จ
3. โหลด artifact "walkietalkie-jar" จะได้ walkietalkie-1.0.0.jar

หรือ build เองในเครื่อง: ติดตั้ง JDK 17 + Gradle 8.5 แล้วรัน `gradle build` ไฟล์จะอยู่ที่ build/libs/

## ติดตั้ง
ใส่ jar นี้ + Plasmo Voice (Forge 1.20.1, 2.1.x) ทั้งฝั่งเซิร์ฟเวอร์และไคลเอนต์

## จุดที่ผมยังตรวจกับ API จริงไม่ได้ (ถ้าคอมไพล์ไม่ผ่านให้แก้ตรงนี้)
- `plasmo_voice_version` ใน gradle.properties: ตั้งเป็น 2.1.5 ถ้า repo ไม่มีเวอร์ชันนี้ให้เปลี่ยนเป็นเวอร์ชันที่มีจริง
- pv/WalkieTalkieAddon.java
  - ชื่อ package ของ AudioEncoder/AudioDecoder (su.plo.voice.api.audio.codec)
  - `uuidOf()` ใช้ `getInstance().getGameProfile().getId()`
- pv/PermissionCompat.java ใช้ reflection ลงทะเบียนสิทธิ์ `pv.addon.walkietalkie.use` ให้ทุกคนใช้ได้
  ถ้าขึ้น warning ใน log ให้ให้สิทธิ์นี้ผ่าน LuckPerms หรือปลั๊กอินสิทธิ์
