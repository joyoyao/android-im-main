//
//  Voip.h
//  Voip
//
//  Created by test on 14-7-15.
//  Copyright (c) 2014-2015 RongCloud. All rights reserved.
//

#ifndef __Voip__Voip__
#define __Voip__Voip__

class IVoipCallback
{
public:
    virtual ~IVoipCallback(){}
    virtual bool Callme(unsigned char* pbData, unsigned long nl) = 0;
    virtual void Error(int eType, const char* pszDescription) = 0;
};

void StartVoip(const char* pszAppId, const char* pszToken, const char* pszFromUserId, const char* pszToUserId, int nLocalPort, IVoipCallback* pCallback);
void AcceptVoip(const char* pszAppId, const char* pszSessionId, const char* pszUserId, const char* pszRemoteIp, int nRemotePort, int nLocalPort, int nRemoteControlPort, IVoipCallback* pCallback);
void EndVoip(const char* pszAppId, const char* pszSessionId, const char* pszUserId, IVoipCallback* pCallback);

#endif /* defined(__Voip__Voip__) */
