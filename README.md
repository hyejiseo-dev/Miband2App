# Miband2App
2019.02-03 miband2기기 데이터를 블루투스 연동하여 추출한 앱 </br>
Blog : https://velog.io/@hyejiseo-dev

## Introduction
샤오미 미밴드2의 데이터를 블루투스로 연동
1. miband2는 다른 웨어러블 디바이스 제작사처럼 공식 SDK가 없어 인터넷에 있는 코드를 참고하였습니다.
2. 데이터 통신(블루투스)을 위해서 심박수, 걸음수, 배터리 잔량 등 각 기능에 맞는 UUID를 이용하였습니다.
3. 파이어베이스와 연동하여 실시간으로 심박수, 걸음수를 표시하였습니다.
4. 이후 다른 서비스의 확장을 위해 기본 연동 기능만 구현하였습니다.

## Development Environment
- Java (minSdk 23 / targetSdk 28)
- firebase RealtimeDatabase 사용
- Bluetooth 활용 실시간 송수신

![Android Studio](https://img.shields.io/badge/Android%20Studio-3DDC84.svg?style=for-the-badge&logo=android-studio&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-039BE5?style=for-the-badge&logo=Firebase&logoColor=white)

## Application Version
- minSdkVersion : 23
- targetSdkVersion : 28

## Database 
Firebase DB 사용

