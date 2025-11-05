package com.github.fhilgers.compose.application.common

//@Composable
//fun RowScope.UserState(
//    userTrustLevelFlow: StateFlow<UserTrustLevel?>,
//    isUserBlockedFlow: StateFlow<Boolean>,
//    membershipFlow: StateFlow<Membership?>? = null,
//    unbannableFlow: StateFlow<Boolean>? = null
//) {
//    val userTrustLevel = userTrustLevelFlow.collectAsState().value
//    val isUserBlocked = isUserBlockedFlow.collectAsState().value
//    val membership = membershipFlow?.collectAsState()?.value
//    val unbannable = unbannableFlow?.collectAsState()?.value == true
//    val i18n = DI.get<I18nView>()
//
//    if (isUserBlocked) {
//        Tooltip({ Text(i18n.roomHeaderUserIsBlocked()) }) {
//            Icon(
//                Icons.Default.Block,
//                i18n.roomHeaderUserIsBlocked(),
//                modifier = Modifier.size(24.dp),
//                tint = MaterialTheme.messengerColors.blockedUser,
//            )
//        }
//        Spacer(Modifier.size(5.dp))
//    } else if (membership == Membership.BAN) {
//        BanIcon(if (unbannable) BanIconType.Unbannable else BanIconType.NotUnbannable, 16.dp)
//    } else if (membership == Membership.KNOCK) {
//        KnockIcon(16.dp)
//    } else {
//        when (userTrustLevel) {
//            is UserTrustLevel.CrossSigned ->
//                if (userTrustLevel.verified) {
//                    VerifiedIcon(VerificationLevel.USER, 16.dp)
//                } else {
//                    NeutralVerifiedIcon(VerificationLevel.USER, 16.dp)
//                }
//
//            is UserTrustLevel.Unknown -> {
//                NeutralVerifiedIcon(VerificationLevel.USER, 16.dp)
//            }
//
//            null -> Box { }
//            else -> {
//                NotVerifiedIcon(VerificationLevel.USER, 16.dp)
//            }
//        }
//    }
//}
