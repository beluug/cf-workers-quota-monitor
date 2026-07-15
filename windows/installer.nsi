Unicode true
!include "MUI2.nsh"

!ifndef ARCH
  !define ARCH "x64"
!endif
!ifndef PUBLISH_DIR
  !error "PUBLISH_DIR is required"
!endif
!ifndef OUTPUT_DIR
  !error "OUTPUT_DIR is required"
!endif
!ifndef APP_ICON
  !error "APP_ICON is required"
!endif

!define PRODUCT_NAME "CF Quota Monitor"
!define PRODUCT_VERSION "1.0.0"
!define PRODUCT_PUBLISHER "CF Quota Monitor Contributors"
!define PRODUCT_EXE "CFQuotaMonitor.exe"

Name "${PRODUCT_NAME}"
OutFile "${OUTPUT_DIR}\CF-Quota-Monitor-v${PRODUCT_VERSION}-Windows-${ARCH}-Setup.exe"
InstallDir "$LOCALAPPDATA\Programs\CFQuotaMonitor"
InstallDirRegKey HKCU "Software\CFQuotaMonitor" "InstallDir"
RequestExecutionLevel user
Icon "${APP_ICON}"
UninstallIcon "${APP_ICON}"
BrandingText "CF Quota Monitor | MIT License"

!define MUI_ABORTWARNING
!define MUI_ICON "${APP_ICON}"
!define MUI_UNICON "${APP_ICON}"
!define MUI_FINISHPAGE_RUN "$INSTDIR\${PRODUCT_EXE}"
!define MUI_FINISHPAGE_RUN_TEXT "Run CF Quota Monitor"
!insertmacro MUI_PAGE_WELCOME
!insertmacro MUI_PAGE_DIRECTORY
!insertmacro MUI_PAGE_COMPONENTS
!insertmacro MUI_PAGE_INSTFILES
!insertmacro MUI_PAGE_FINISH
!insertmacro MUI_UNPAGE_CONFIRM
!insertmacro MUI_UNPAGE_INSTFILES
!insertmacro MUI_LANGUAGE "English"
!insertmacro MUI_LANGUAGE "SimpChinese"

Section "CF Quota Monitor (required)" SecApp
  SectionIn RO
  SetOutPath "$INSTDIR"
  File /r "${PUBLISH_DIR}\*.*"
  WriteUninstaller "$INSTDIR\Uninstall.exe"
  WriteRegStr HKCU "Software\CFQuotaMonitor" "InstallDir" "$INSTDIR"
  CreateDirectory "$SMPROGRAMS\CF Quota Monitor"
  CreateShortcut "$SMPROGRAMS\CF Quota Monitor\CF Quota Monitor.lnk" "$INSTDIR\${PRODUCT_EXE}"
  CreateShortcut "$SMPROGRAMS\CF Quota Monitor\Uninstall.lnk" "$INSTDIR\Uninstall.exe"

  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CFQuotaMonitor" "DisplayName" "${PRODUCT_NAME}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CFQuotaMonitor" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CFQuotaMonitor" "Publisher" "${PRODUCT_PUBLISHER}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CFQuotaMonitor" "DisplayIcon" "$INSTDIR\${PRODUCT_EXE}"
  WriteRegStr HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CFQuotaMonitor" "UninstallString" '"$INSTDIR\Uninstall.exe"'
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CFQuotaMonitor" "NoModify" 1
  WriteRegDWORD HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CFQuotaMonitor" "NoRepair" 1
SectionEnd

Section /o "Desktop shortcut" SecDesktop
  CreateShortcut "$DESKTOP\CF Quota Monitor.lnk" "$INSTDIR\${PRODUCT_EXE}"
SectionEnd

LangString DESC_SecApp ${LANG_ENGLISH} "Install the application for the current Windows user."
LangString DESC_SecApp ${LANG_SIMPCHINESE} "Install the application for the current Windows user."
LangString DESC_SecDesktop ${LANG_ENGLISH} "Create a shortcut on the desktop."
LangString DESC_SecDesktop ${LANG_SIMPCHINESE} "Create a shortcut on the desktop."
!insertmacro MUI_FUNCTION_DESCRIPTION_BEGIN
  !insertmacro MUI_DESCRIPTION_TEXT ${SecApp} $(DESC_SecApp)
  !insertmacro MUI_DESCRIPTION_TEXT ${SecDesktop} $(DESC_SecDesktop)
!insertmacro MUI_FUNCTION_DESCRIPTION_END

Section "Uninstall"
  Delete "$DESKTOP\CF Quota Monitor.lnk"
  Delete "$SMPROGRAMS\CF Quota Monitor\CF Quota Monitor.lnk"
  Delete "$SMPROGRAMS\CF Quota Monitor\Uninstall.lnk"
  RMDir "$SMPROGRAMS\CF Quota Monitor"
  DeleteRegKey HKCU "Software\Microsoft\Windows\CurrentVersion\Uninstall\CFQuotaMonitor"
  DeleteRegKey HKCU "Software\CFQuotaMonitor"
  DeleteRegValue HKCU "Software\Microsoft\Windows\CurrentVersion\Run" "CFQuotaMonitor"
  RMDir /r "$INSTDIR"
SectionEnd
