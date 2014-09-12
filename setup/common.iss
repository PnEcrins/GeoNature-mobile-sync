; Script generated by the Inno Setup Script Wizard.
; SEE THE DOCUMENTATION FOR DETAILS ON CREATING INNO SETUP SCRIPT FILES!

#define MyAppName "Sync"
#define MyAppVersion "0.2.5"
#define MyAppPublisher "Makina Corpus"
#define MyAppExeName "sync.exe"
#define MyAppHelpFileName "sync.pdf"

[Setup]
; NOTE: The value of AppId uniquely identifies this application.
; Do not use the same AppId value in installers for other applications.
; (To generate a new GUID, click Tools | Generate GUID inside the IDE.)
AppId={{C04E708B-1243-4AC1-A1D4-E8CEABD03579}
AppName={#MyAppName}
AppVersion={#MyAppVersion}
;AppVerName={#MyAppName} {#MyAppVersion}
AppPublisher={#MyAppPublisher}
DefaultDirName={pf}\PNE Sync
DefaultGroupName={#MyAppName}
OutputBaseFilename=setup_sync-{#MyAppVersion}-{#Arch}
LicenseFile=LICENSE.txt
SetupIconFile=../art/sync.ico
Compression=lzma
SolidCompression=yes
VersionInfoVersion={#MyAppVersion}
VersionInfoTextVersion={#MyAppVersion}

[Languages]
Name: "english"; MessagesFile: "compiler:Default.isl"
Name: "french"; MessagesFile: "compiler:Languages\French.isl"

#include ReadReg(HKEY_LOCAL_MACHINE,'Software\Sherlock Software\InnoTools\Downloader','ScriptPath','');

[Types]
Name: "full"; Description: "Full installation"
Name: "compact"; Description: "Compact installation"
Name: "custom"; Description: "Custom installation"; Flags: iscustom

[Components]
Name: "program"; Description: "Program Files"; Types: full compact custom; Flags: fixed
Name: "drivers"; Description: "Google USB Driver"; Types: full; ExtraDiskSpaceRequired: 8682752

[Tasks]
Name: "desktopicon"; Description: "{cm:CreateDesktopIcon}"; GroupDescription: "{cm:AdditionalIcons}"; Components: program; Flags: unchecked

[Files]
Source: "unzip.exe"; DestDir: "{tmp}"; Flags: ignoreversion
Source: "sync.pdf"; DestDir: "{app}"; Flags: ignoreversion
Source: "sync.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "../target/sync-{#MyAppVersion}-win32-{#Arch}.jar"; DestDir: "{app}"; Flags: ignoreversion
Source: "LICENSE.txt"; DestDir: "{app}"; Flags: ignoreversion
; NOTE: Don't use "Flags: ignoreversion" on any shared system files

[Dirs]
Name: "{app}\usb_driver"

[Icons]
Name: "{group}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"
Name: "{group}\{cm:UninstallProgram,{#MyAppName}}"; Filename: "{uninstallexe}"
Name: "{group}\{#MyAppHelpFileName}"; Filename: "{app}\{#MyAppHelpFileName}"
Name: "{commondesktop}\{#MyAppName}"; Filename: "{app}\{#MyAppExeName}"; Tasks: desktopicon

[Code]
 
function LoadValueFromXML(const AFileName, APath: string): string;
var
  XMLNode: Variant;
  XMLDocument: Variant;
begin
  Result := '';
  XMLDocument := CreateOleObject('Msxml2.DOMDocument.6.0');
  try
    XMLDocument.async := False;
    XMLDocument.load(AFileName);
    if (XMLDocument.parseError.errorCode <> 0) then
      MsgBox('The XML file could not be parsed. ' + XMLDocument.parseError.reason, mbError, MB_OK)
    else
    begin
      XMLDocument.setProperty('SelectionLanguage', 'XPath');
      XMLDocument.setProperty('SelectionNamespaces', 'xmlns:sdk="http://schemas.android.com/sdk/android/addon/7"');
      XMLNode := XMLDocument.selectSingleNode(APath);
      Result := XMLNode.text;
    end;
  except
    MsgBox('Unable to load ' + AFileName, mbError, MB_OK);
  end;
end;

function GetUsbDriverFile(): string;
begin
  Result := LoadValueFromXML(expandconstant('{tmp}\addon.xml'), '//sdk:sdk-addon/sdk:extra[sdk:path="usb_driver"]/sdk:archives/sdk:archive[sdk:host-os="windows"]/sdk:url');
end;

Procedure DownloadUSBDriver();
var
  UsbDriverFile: string;
begin
  UsbDriverFile := GetUsbDriverFile();

  if (length(UsbDriverFile) > 0) then begin
    Log('downloading file ' + 'http://dl-ssl.google.com/android/repository/' + UsbDriverFile + ' ...');
    ITD_AddFile('http://dl-ssl.google.com/android/repository/' + UsbDriverFile, expandconstant('{tmp}\') + UsbDriverFile);
    ITD_DownloadAfter(wpReady);
  end;
end;

Procedure ExtractUSBDriver();
var
  UsbDriverFile: string;
  ResultCode: Integer;
begin
  UsbDriverFile := GetUsbDriverFile();
  
  if (length(UsbDriverFile) > 0) then begin
    FileCopy(expandconstant('{tmp}\') + UsbDriverFile, expandconstant('{app}\') + UsbDriverFile, false);
    Log('extracting file ' + expandconstant('{app}\') + UsbDriverFile + ' ...');
    if FileExists(expandconstant('{app}\') + UsbDriverFile) then begin
      Log('extracting file ' + UsbDriverFile + ' ...');
      Exec(expandconstant('{tmp}\unzip.exe'), '-oq ' + UsbDriverFile, expandconstant('{app}'), SW_HIDE, ewWaitUntilTerminated, ResultCode);
      DeleteFile(expandconstant('{app}\') + UsbDriverFile);
    end
    else
      Log('failed to extract ' + UsbDriverFile);
  end;  
end;

function NextButtonClick(CurPageID: Integer): Boolean;
begin
  Result := True;

  if CurPageID = wpSelectComponents then
  begin
    if WizardForm.ComponentsList.Checked[1] then
    begin
      // downloads addon.xml file from Google
      ITD_DownloadFile('http://dl-ssl.google.com/android/repository/addon.xml', expandconstant('{tmp}\addon.xml'));
      DownloadUSBDriver();
    end
    else
    begin
      ITD_ClearFiles;
    end;
  end;
end;

procedure InitializeWizard();
begin
  ITD_Init;
end;

procedure CurStepChanged(CurStep: TSetupStep);
begin
  if CurStep=ssPostInstall then
  begin
    if WizardForm.ComponentsList.Checked[1] then
    begin
      ExtractUSBDriver();
    end;
  end;
end;

[Run]
Filename: "{app}\{#MyAppExeName}"; Description: "{cm:LaunchProgram,{#StringChange(MyAppName, '&', '&&')}}"; Flags: nowait postinstall skipifsilent

[UninstallDelete]
Type: filesandordirs; Name: "{app}\usb_driver"