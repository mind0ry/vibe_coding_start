스포츠 뉴스 요약 서비스
=====================

이 프로젝트는 **Spring Boot + React(TanStack Query) + PostgreSQL** 기반으로,
네이버 뉴스 API를 이용해 최근 스포츠 이슈(축구/야구/농구/배구)를 요약해서 보여주는 데모입니다.

## 구조

- `backend` : Spring Boot (Java) + PostgreSQL
- `frontend` : React + Vite + TypeScript + TanStack Query

## 네이버 API 키 설정

백엔드의 네이버 뉴스 연동 부분은 **API 키를 직접 채워야** 동작합니다.

- `backend/src/main/resources/application-example.yml` 파일을 참고해서
  `application.yml` 또는 환경 변수로 키를 설정해 주세요.

