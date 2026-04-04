import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withFetch } from '@angular/common/http';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { routes } from './app.routes';
import { en_US, provideNzI18n } from 'ng-zorro-antd/i18n';
import { registerLocaleData } from '@angular/common';
import en from '@angular/common/locales/en';
import { provideNzIcons } from 'ng-zorro-antd/icon';
import {
  DashboardOutline,
  TeamOutline,
  UserOutline,
  CalendarOutline,
  BarChartOutline,
  SettingOutline,
  BellOutline,
  AuditOutline,
  ScheduleOutline,
  MenuFoldOutline,
  MenuUnfoldOutline,
  PlusOutline,
  EditOutline,
  DeleteOutline,
  SearchOutline,
  CheckCircleOutline,
  CloseCircleOutline,
  ExclamationCircleOutline,
  ReloadOutline,
  UploadOutline,
  EyeOutline,
  ArrowLeftOutline,
  CameraOutline,
  PieChartOutline,
  ClusterOutline,
  ApartmentOutline,
  AlertOutline,
  ProjectOutline,
  RiseOutline,
  ToolOutline,
  InfoCircleOutline,
  WarningOutline,
  DollarOutline,
  AppstoreOutline,
  InboxOutline,
} from '@ant-design/icons-angular/icons';

registerLocaleData(en);

const icons = [
  DashboardOutline,
  TeamOutline,
  UserOutline,
  CalendarOutline,
  BarChartOutline,
  SettingOutline,
  BellOutline,
  AuditOutline,
  ScheduleOutline,
  MenuFoldOutline,
  MenuUnfoldOutline,
  PlusOutline,
  EditOutline,
  DeleteOutline,
  SearchOutline,
  CheckCircleOutline,
  CloseCircleOutline,
  ExclamationCircleOutline,
  ReloadOutline,
  UploadOutline,
  EyeOutline,
  ArrowLeftOutline,
  CameraOutline,
  PieChartOutline,
  ClusterOutline,
  ApartmentOutline,
  AlertOutline,
  ProjectOutline,
  RiseOutline,
  ToolOutline,
  InfoCircleOutline,
  WarningOutline,
  DollarOutline,
  AppstoreOutline,
  InboxOutline,
];

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(withFetch()),
    provideAnimationsAsync(),
    provideNzI18n(en_US),
    provideNzIcons(icons),
  ],
};
