#version 330

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
#moj_import <minecraft:fog.glsl>
#moj_import <minecraft:sample_lightmap.glsl>
#endif

#moj_import <minecraft:dynamictransforms.glsl>
#moj_import <minecraft:projection.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
in ivec2 UV2;
#endif

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
uniform sampler2D Sampler2;
out float sphericalVertexDistance;
out float cylindricalVertexDistance;
#endif

out vec4 vertexColor;
out vec2 texCoord0;

#moj_import <minecraft:globals.glsl>
uniform sampler2D Sampler0;
out vec2 texCoord1;
out vec2 texCoord2;
out vec3 minimapCarrierPosition;
out float minimap;
out float minimapShape;
out float keepEdges;
out float frameStyle;
out float frameThickness;
out float colorProfile;
out float transition;
out float fullscreenMinimap;
out float sx;
out float sy;
#moj_import <minecraft:minimap/vertex_util.glsl>
#define EH_VERSION 4
#define EH_OFFSET 29

float get_id(float offset) {
    if (offset <= 0.0) return 0.0;
    return trunc(offset / 1000.0);
}


void main() {
    vec3 pos = Position;
#ifdef IS_GUI


    vec2 pixel = vec2(ProjMat[0][0], ProjMat[1][1]) / 2.0;
    int guiScale = max(1, int(round(pixel.x * ScreenSize.x)));
    vec2 guiSize = ScreenSize / guiScale;

    float id = get_id((round(EH_OFFSET - Position.y)) * -1.0);

    if (id > 99.0 && id < 500.0 && Color.a != 0.0) {
        float yOffset = 0.0;
        float xOffset = 0.0;
        float layer = 0.0;
        vec2 scale = vec2(1.0, 1.0);
        bool outlined = false;

        switch (int(id)) {
            case 100:
                xOffset = int(guiSize.x * (-7.0/100))-53;
                yOffset = int(guiSize.y * (1.0/100))+10;
                layer = 0.0;
                outlined = true;
                break;
            case 101:
                xOffset = int(guiSize.x * (-7.0/100))-69;
                yOffset = int(guiSize.y * (1.0/100));
                layer = 0.0;
                outlined = true;
                break;
            case 102:
                xOffset = int(guiSize.x * (-0.0/100))-65;
                yOffset = int(guiSize.y * (100.0/100))-26;
                layer = 0.0;
                break;
            case 103:
                xOffset = int(guiSize.x * (-0.0/100))-102;
                yOffset = int(guiSize.y * (100.0/100))-76;
                layer = 0.0;
                outlined = true;
                break;
            case 104:
                xOffset = int(guiSize.x * (-50.0/100))-3;
                yOffset = int(guiSize.y * (0.0/100))+44;
                layer = 0.0;
                break;
            case 105:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+44;
                layer = 8.0;
                outlined = true;
                break;
            case 106:
                xOffset = int(guiSize.x * (-0.3/100))-43;
                yOffset = int(guiSize.y * (100.0/100))-9;
                layer = 0.0;
                break;
            case 107:
                xOffset = int(guiSize.x * (-0.0/100))-205;
                yOffset = int(guiSize.y * (100.0/100))-37;
                layer = 8.0;
                outlined = true;
                break;
            case 108:
                xOffset = int(guiSize.x * (-0.3/100))-247;
                yOffset = int(guiSize.y * (100.0/100))-14;
                layer = 0.0;
                break;
            case 109:
                xOffset = int(guiSize.x * (-0.0/100))-216;
                yOffset = int(guiSize.y * (100.0/100))-16;
                layer = 8.0;
                outlined = true;
                break;
            case 110:
                xOffset = int(guiSize.x * (-0.0/100))-240;
                yOffset = int(guiSize.y * (100.0/100))-37;
                layer = 0.0;
                break;
            case 111:
                xOffset = int(guiSize.x * (-0.3/100))-240;
                yOffset = int(guiSize.y * (100.0/100))-27;
                layer = 0.0;
                break;
            case 112:
                xOffset = int(guiSize.x * (-0.3/100))-208;
                yOffset = int(guiSize.y * (100.0/100))-27;
                layer = 8.0;
                outlined = true;
                break;
            case 113:
                xOffset = int(guiSize.x * (-0.3/100))-196;
                yOffset = int(guiSize.y * (100.0/100))+1;
                layer = 0.0;
                break;
            case 114:
                xOffset = int(guiSize.x * (-48.0/100))-8;
                yOffset = int(guiSize.y * (95.0/100))+3;
                layer = 0.0;
                outlined = true;
                break;
            case 115:
                xOffset = int(guiSize.x * (-94.0/100))-55;
                yOffset = int(guiSize.y * (95.0/100))-12;
                layer = 0.0;
                break;
            case 116:
                xOffset = int(guiSize.x * (-94.0/100))-55;
                yOffset = int(guiSize.y * (95.0/100));
                layer = 0.0;
                outlined = true;
                break;
            case 117:
                xOffset = int(guiSize.x * (-50.0/100))-18;
                yOffset = int(guiSize.y * (89.0/100))-24;
                layer = 0.0;
                outlined = true;
                break;
            case 118:
                xOffset = int(guiSize.x * (-50.0/100))-18;
                yOffset = int(guiSize.y * (89.0/100))-12;
                layer = 6.0;
                break;
            case 119:
                xOffset = int(guiSize.x * (-50.0/100))-18;
                yOffset = int(guiSize.y * (89.0/100));
                layer = 0.0;
                outlined = true;
                break;
        }

        if (true) {
            pos.y -= (id * 1000.0) + 500.0 + EH_OFFSET;
            pos.x -= (guiSize.x * 0.5);

            pos.x *= scale.x;
            pos.y *= scale.y;

            pos.y += guiSize.y;
            if (guiScale == 3) {
                pos.x += 1.45;
            }

            pos -= vec3(xOffset, yOffset, 0.0);
            pos.z += layer;
        }
    }
    // HappyHUD action-bar glyphs use a separate ID range from AnchorHUD.
    float happyId = get_id((round(guiSize.y - Position.y + 64.0)) * -1.0);
    if (happyId >= 500.0 && Color.a != 0.0) {
        float yOffset = 0.0;
        float xOffset = 0.0;
        int layer = 0;
        bool outlined = false;
        switch (int(happyId)) {
            case 500:
                xOffset = int(guiSize.x * (-50.0/100))-290;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 1;
                break;
            case 501:
                xOffset = int(guiSize.x * (-50.0/100))-290;
                yOffset = int(guiSize.y * (0.0/100))+14;
                layer = 1;
                break;
            case 502:
                xOffset = int(guiSize.x * (-50.0/100))-279;
                yOffset = int(guiSize.y * (0.0/100))+12;
                layer = 2;
                break;
            case 503:
                xOffset = int(guiSize.x * (-50.0/100))-290;
                yOffset = int(guiSize.y * (0.0/100))+21;
                layer = 1;
                break;
            case 504:
                xOffset = int(guiSize.x * (-50.0/100))-290;
                yOffset = int(guiSize.y * (0.0/100))+24;
                layer = 1;
                break;
            case 505:
                xOffset = int(guiSize.x * (-50.0/100))-279;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 506:
                xOffset = int(guiSize.x * (-50.0/100))-260;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 1;
                break;
            case 507:
                xOffset = int(guiSize.x * (-50.0/100))-260;
                yOffset = int(guiSize.y * (0.0/100))+14;
                layer = 1;
                break;
            case 508:
                xOffset = int(guiSize.x * (-50.0/100))-249;
                yOffset = int(guiSize.y * (0.0/100))+12;
                layer = 2;
                break;
            case 509:
                xOffset = int(guiSize.x * (-50.0/100))-260;
                yOffset = int(guiSize.y * (0.0/100))+21;
                layer = 1;
                break;
            case 510:
                xOffset = int(guiSize.x * (-50.0/100))-260;
                yOffset = int(guiSize.y * (0.0/100))+24;
                layer = 1;
                break;
            case 511:
                xOffset = int(guiSize.x * (-50.0/100))-249;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 512:
                xOffset = int(guiSize.x * (-50.0/100))-216;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 1;
                break;
            case 513:
                xOffset = int(guiSize.x * (-50.0/100))-216;
                yOffset = int(guiSize.y * (0.0/100))+14;
                layer = 1;
                break;
            case 514:
                xOffset = int(guiSize.x * (-50.0/100))-205;
                yOffset = int(guiSize.y * (0.0/100))+12;
                layer = 2;
                break;
            case 515:
                xOffset = int(guiSize.x * (-50.0/100))-216;
                yOffset = int(guiSize.y * (0.0/100))+21;
                layer = 1;
                break;
            case 516:
                xOffset = int(guiSize.x * (-50.0/100))-216;
                yOffset = int(guiSize.y * (0.0/100))+24;
                layer = 1;
                break;
            case 517:
                xOffset = int(guiSize.x * (-50.0/100))-205;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 518:
                xOffset = int(guiSize.x * (-50.0/100))-172;
                yOffset = int(guiSize.y * (0.0/100))+23;
                layer = 1;
                break;
            case 519:
                xOffset = int(guiSize.x * (-50.0/100))-173;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 520:
                xOffset = int(guiSize.x * (-50.0/100))-162;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 3;
                break;
            case 521:
                xOffset = int(guiSize.x * (-50.0/100))-130;
                yOffset = int(guiSize.y * (0.0/100))+23;
                layer = 1;
                break;
            case 522:
                xOffset = int(guiSize.x * (-50.0/100))-131;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 523:
                xOffset = int(guiSize.x * (-50.0/100))-120;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 3;
                break;
            case 524:
                xOffset = int(guiSize.x * (-50.0/100))+210;
                yOffset = int(guiSize.y * (0.0/100))+23;
                layer = 1;
                break;
            case 525:
                xOffset = int(guiSize.x * (-50.0/100))+209;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 2;
                break;
            case 526:
                xOffset = int(guiSize.x * (-50.0/100))+209;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 3;
                break;
            case 527:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+21;
                layer = 4;
                break;
            case 528:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+24;
                layer = 4;
                break;
            case 529:
                xOffset = int(guiSize.x * (-50.0/100))+220;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 5;
                break;
            case 530:
                xOffset = int(guiSize.x * (-50.0/100))-130;
                yOffset = int(guiSize.y * (0.0/100))+12;
                layer = 1;
                break;
            case 531:
                xOffset = int(guiSize.x * (-50.0/100))-131;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 2;
                break;
            case 532:
                xOffset = int(guiSize.x * (-50.0/100))-151;
                yOffset = int(guiSize.y * (0.0/100))+10;
                layer = 3;
                break;
            case 533:
                xOffset = int(guiSize.x * (-50.0/100))-151;
                yOffset = int(guiSize.y * (0.0/100))+13;
                layer = 3;
                break;
            case 534:
                xOffset = int(guiSize.x * (-50.0/100))-120;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 4;
                break;
            case 535:
                xOffset = int(guiSize.x * (-50.0/100))+210;
                yOffset = int(guiSize.y * (0.0/100))+12;
                layer = 1;
                break;
            case 536:
                xOffset = int(guiSize.x * (-50.0/100))+209;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 2;
                break;
            case 537:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+10;
                layer = 3;
                break;
            case 538:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+13;
                layer = 3;
                break;
            case 539:
                xOffset = int(guiSize.x * (-50.0/100))+220;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 4;
                break;
            case 540:
                xOffset = int(guiSize.x * (-50.0/100))-138;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 1;
                break;
            case 541:
                xOffset = int(guiSize.x * (-50.0/100))-136;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 2;
                break;
            case 542:
                xOffset = int(guiSize.x * (-50.0/100))-138;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 3;
                break;
            case 543:
                xOffset = int(guiSize.x * (-50.0/100))-138;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 4;
                break;
            case 544:
                xOffset = int(guiSize.x * (-50.0/100))-146;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 5;
                break;
            case 545:
                xOffset = int(guiSize.x * (-50.0/100))-146;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 5;
                break;
            case 546:
                xOffset = int(guiSize.x * (-50.0/100))-111;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 6;
                break;
            case 547:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 7;
                break;
            case 548:
                xOffset = int(guiSize.x * (-50.0/100))-111;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 8;
                break;
            case 549:
                xOffset = int(guiSize.x * (-50.0/100))-111;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 9;
                break;
            case 550:
                xOffset = int(guiSize.x * (-50.0/100))-119;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 10;
                break;
            case 551:
                xOffset = int(guiSize.x * (-50.0/100))-119;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 10;
                break;
            case 552:
                xOffset = int(guiSize.x * (-50.0/100))-84;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 11;
                break;
            case 553:
                xOffset = int(guiSize.x * (-50.0/100))-82;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 12;
                break;
            case 554:
                xOffset = int(guiSize.x * (-50.0/100))-84;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 13;
                break;
            case 555:
                xOffset = int(guiSize.x * (-50.0/100))-84;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 14;
                break;
            case 556:
                xOffset = int(guiSize.x * (-50.0/100))-92;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 15;
                break;
            case 557:
                xOffset = int(guiSize.x * (-50.0/100))-92;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 15;
                break;
            case 558:
                xOffset = int(guiSize.x * (-50.0/100))-57;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 16;
                break;
            case 559:
                xOffset = int(guiSize.x * (-50.0/100))-55;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 17;
                break;
            case 560:
                xOffset = int(guiSize.x * (-50.0/100))-57;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 18;
                break;
            case 561:
                xOffset = int(guiSize.x * (-50.0/100))-57;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 19;
                break;
            case 562:
                xOffset = int(guiSize.x * (-50.0/100))-65;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 20;
                break;
            case 563:
                xOffset = int(guiSize.x * (-50.0/100))-65;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 20;
                break;
            case 564:
                xOffset = int(guiSize.x * (-50.0/100))-30;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 21;
                break;
            case 565:
                xOffset = int(guiSize.x * (-50.0/100))-28;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 22;
                break;
            case 566:
                xOffset = int(guiSize.x * (-50.0/100))-30;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 23;
                break;
            case 567:
                xOffset = int(guiSize.x * (-50.0/100))-30;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 24;
                break;
            case 568:
                xOffset = int(guiSize.x * (-50.0/100))-38;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 25;
                break;
            case 569:
                xOffset = int(guiSize.x * (-50.0/100))-38;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 25;
                break;
            case 570:
                xOffset = int(guiSize.x * (-50.0/100))-3;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 26;
                break;
            case 571:
                xOffset = int(guiSize.x * (-50.0/100))-1;
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 27;
                break;
            case 572:
                xOffset = int(guiSize.x * (-50.0/100))-3;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 28;
                break;
            case 573:
                xOffset = int(guiSize.x * (-50.0/100))-3;
                yOffset = int(guiSize.y * (0.0/100))+73;
                layer = 29;
                break;
            case 574:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+69;
                layer = 30;
                break;
            case 575:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+72;
                layer = 30;
                break;
            case 576:
                xOffset = int(guiSize.x * (-50.0/100))-144;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 31;
                break;
            case 577:
                xOffset = int(guiSize.x * (-50.0/100))-117;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 32;
                break;
            case 578:
                xOffset = int(guiSize.x * (-50.0/100))-90;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 33;
                break;
            case 579:
                xOffset = int(guiSize.x * (-50.0/100))-63;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 34;
                break;
            case 580:
                xOffset = int(guiSize.x * (-50.0/100))-36;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 35;
                break;
            case 581:
                xOffset = int(guiSize.x * (-50.0/100))-9;
                yOffset = int(guiSize.y * (0.0/100))+59;
                layer = 36;
                break;
            case 582:
                xOffset = int(guiSize.x * (-50.0/100))-7;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 583:
                xOffset = int(guiSize.x * (-50.0/100))-7;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 584:
                xOffset = int(guiSize.x * (-50.0/100))+4;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 585:
                xOffset = int(guiSize.x * (-50.0/100))+25;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 586:
                xOffset = int(guiSize.x * (-50.0/100))+25;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 587:
                xOffset = int(guiSize.x * (-50.0/100))+36;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 588:
                xOffset = int(guiSize.x * (-50.0/100))+57;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 589:
                xOffset = int(guiSize.x * (-50.0/100))+57;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 590:
                xOffset = int(guiSize.x * (-50.0/100))+68;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 591:
                xOffset = int(guiSize.x * (-50.0/100))+89;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 592:
                xOffset = int(guiSize.x * (-50.0/100))+89;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 593:
                xOffset = int(guiSize.x * (-50.0/100))+100;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 594:
                xOffset = int(guiSize.x * (-50.0/100))+209;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 3;
                break;
            case 595:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+10;
                layer = 4;
                break;
            case 596:
                xOffset = int(guiSize.x * (-50.0/100))+189;
                yOffset = int(guiSize.y * (0.0/100))+13;
                layer = 4;
                break;
            case 597:
                xOffset = int(guiSize.x * (-50.0/100))+220;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 5;
                break;
            case 598:
                xOffset = int(guiSize.x * (-50.0/100))-120;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 3;
                break;
            case 599:
                xOffset = int(guiSize.x * (-50.0/100))+220;
                yOffset = int(guiSize.y * (0.0/100))+22;
                layer = 3;
                break;
            case 600:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+8;
                layer = 1;
                break;
            case 601:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 1;
                break;
            case 602:
                xOffset = int(guiSize.x * (-50.0/100))-132;
                yOffset = int(guiSize.y * (0.0/100))+9;
                layer = 2;
                break;
            case 603:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+8;
                layer = 1;
                break;
            case 604:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+11;
                layer = 1;
                break;
            case 605:
                xOffset = int(guiSize.x * (-50.0/100))-98;
                yOffset = int(guiSize.y * (0.0/100))+9;
                layer = 2;
                break;
            case 606:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+28;
                layer = 1;
                break;
            case 607:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+31;
                layer = 1;
                break;
            case 608:
                xOffset = int(guiSize.x * (-50.0/100))-132;
                yOffset = int(guiSize.y * (0.0/100))+29;
                layer = 2;
                break;
            case 609:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+28;
                layer = 1;
                break;
            case 610:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+31;
                layer = 1;
                break;
            case 611:
                xOffset = int(guiSize.x * (-50.0/100))-98;
                yOffset = int(guiSize.y * (0.0/100))+29;
                layer = 2;
                break;
            case 612:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 613:
                xOffset = int(guiSize.x * (-50.0/100))-143;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 614:
                xOffset = int(guiSize.x * (-50.0/100))-132;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 615:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 1;
                break;
            case 616:
                xOffset = int(guiSize.x * (-50.0/100))-109;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 1;
                break;
            case 617:
                xOffset = int(guiSize.x * (-50.0/100))-98;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 618:
                xOffset = int(guiSize.x * (-50.0/100))-52;
                yOffset = int(guiSize.y * (0.0/100))+50;
                layer = 1;
                break;
            case 619:
                xOffset = int(guiSize.x * (-50.0/100))-53;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 620:
                xOffset = int(guiSize.x * (-50.0/100))-42;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 3;
                break;
            case 621:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+50;
                layer = 1;
                break;
            case 622:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 623:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 3;
                break;
            case 624:
                xOffset = int(guiSize.x * (-50.0/100))+90;
                yOffset = int(guiSize.y * (0.0/100))+50;
                layer = 1;
                break;
            case 625:
                xOffset = int(guiSize.x * (-50.0/100))+89;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 626:
                xOffset = int(guiSize.x * (-50.0/100))+89;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 3;
                break;
            case 627:
                xOffset = int(guiSize.x * (-50.0/100))+69;
                yOffset = int(guiSize.y * (0.0/100))+48;
                layer = 4;
                break;
            case 628:
                xOffset = int(guiSize.x * (-50.0/100))+69;
                yOffset = int(guiSize.y * (0.0/100))+51;
                layer = 4;
                break;
            case 629:
                xOffset = int(guiSize.x * (-50.0/100))+100;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 5;
                break;
            case 630:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+39;
                layer = 1;
                break;
            case 631:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 2;
                break;
            case 632:
                xOffset = int(guiSize.x * (-50.0/100))-31;
                yOffset = int(guiSize.y * (0.0/100))+37;
                layer = 3;
                break;
            case 633:
                xOffset = int(guiSize.x * (-50.0/100))-31;
                yOffset = int(guiSize.y * (0.0/100))+40;
                layer = 3;
                break;
            case 634:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 4;
                break;
            case 635:
                xOffset = int(guiSize.x * (-50.0/100))+90;
                yOffset = int(guiSize.y * (0.0/100))+39;
                layer = 1;
                break;
            case 636:
                xOffset = int(guiSize.x * (-50.0/100))+89;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 2;
                break;
            case 637:
                xOffset = int(guiSize.x * (-50.0/100))+69;
                yOffset = int(guiSize.y * (0.0/100))+37;
                layer = 3;
                break;
            case 638:
                xOffset = int(guiSize.x * (-50.0/100))+69;
                yOffset = int(guiSize.y * (0.0/100))+40;
                layer = 3;
                break;
            case 639:
                xOffset = int(guiSize.x * (-50.0/100))+100;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 4;
                break;
            case 640:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+65;
                layer = 1;
                break;
            case 641:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+65;
                layer = 2;
                break;
            case 642:
                xOffset = int(guiSize.x * (-50.0/100))-5;
                yOffset = int(guiSize.y * (0.0/100))+67;
                layer = 3;
                break;
            case 643:
                xOffset = int(guiSize.x * (-50.0/100))+10;
                yOffset = int(guiSize.y * (0.0/100))+65;
                layer = 1;
                break;
            case 644:
                xOffset = int(guiSize.x * (-50.0/100))+10;
                yOffset = int(guiSize.y * (0.0/100))+65;
                layer = 2;
                break;
            case 645:
                xOffset = int(guiSize.x * (-50.0/100))+15;
                yOffset = int(guiSize.y * (0.0/100))+67;
                layer = 3;
                break;
            case 646:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+55;
                layer = 1;
                break;
            case 647:
                xOffset = int(guiSize.x * (-50.0/100))-10;
                yOffset = int(guiSize.y * (0.0/100))+55;
                layer = 2;
                break;
            case 648:
                xOffset = int(guiSize.x * (-50.0/100))-5;
                yOffset = int(guiSize.y * (0.0/100))+57;
                layer = 3;
                break;
            case 649:
                xOffset = int(guiSize.x * (-50.0/100))+10;
                yOffset = int(guiSize.y * (0.0/100))+55;
                layer = 1;
                break;
            case 650:
                xOffset = int(guiSize.x * (-50.0/100))+10;
                yOffset = int(guiSize.y * (0.0/100))+55;
                layer = 2;
                break;
            case 651:
                xOffset = int(guiSize.x * (-50.0/100))+15;
                yOffset = int(guiSize.y * (0.0/100))+57;
                layer = 3;
                break;
            case 652:
                xOffset = int(guiSize.x * (-0.0/100))-25;
                yOffset = int(guiSize.y * (100.0/100))-25;
                layer = 1;
                break;
            case 653:
                xOffset = int(guiSize.x * (-0.0/100))-27;
                yOffset = int(guiSize.y * (100.0/100))-37;
                layer = 2;
                break;
            case 654:
                xOffset = int(guiSize.x * (-0.0/100))-28;
                yOffset = int(guiSize.y * (100.0/100))-45;
                layer = 3;
                outlined = true;
                break;
            case 655:
                xOffset = int(guiSize.x * (-0.0/100))-28;
                yOffset = int(guiSize.y * (100.0/100))-42;
                layer = 3;
                outlined = true;
                break;
            case 656:
                xOffset = int(guiSize.x * (-0.0/100))-27;
                yOffset = int(guiSize.y * (100.0/100))-31;
                layer = 4;
                break;
            case 657:
                xOffset = int(guiSize.x * (-0.0/100))-28;
                yOffset = int(guiSize.y * (100.0/100))-21;
                layer = 5;
                outlined = true;
                break;
            case 658:
                xOffset = int(guiSize.x * (-0.0/100))-28;
                yOffset = int(guiSize.y * (100.0/100))-18;
                layer = 5;
                outlined = true;
                break;
            case 659:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+50;
                layer = 1;
                break;
            case 660:
                xOffset = int(guiSize.x * (-50.0/100))-12;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 661:
                xOffset = int(guiSize.x * (-50.0/100))-1;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 3;
                break;
            case 662:
                xOffset = int(guiSize.x * (-50.0/100))+91;
                yOffset = int(guiSize.y * (0.0/100))+39;
                layer = 1;
                break;
            case 663:
                xOffset = int(guiSize.x * (-50.0/100))+90;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 2;
                break;
            case 664:
                xOffset = int(guiSize.x * (-50.0/100))+90;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 3;
                break;
            case 665:
                xOffset = int(guiSize.x * (-50.0/100))+70;
                yOffset = int(guiSize.y * (0.0/100))+37;
                layer = 4;
                break;
            case 666:
                xOffset = int(guiSize.x * (-50.0/100))+70;
                yOffset = int(guiSize.y * (0.0/100))+40;
                layer = 4;
                break;
            case 667:
                xOffset = int(guiSize.x * (-50.0/100))+101;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 5;
                break;
            case 668:
                xOffset = int(guiSize.x * (-50.0/100))-11;
                yOffset = int(guiSize.y * (0.0/100))+39;
                layer = 1;
                break;
            case 669:
                xOffset = int(guiSize.x * (-50.0/100))-12;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 2;
                break;
            case 670:
                xOffset = int(guiSize.x * (-50.0/100))-1;
                yOffset = int(guiSize.y * (0.0/100))+38;
                layer = 3;
                break;
            case 671:
                xOffset = int(guiSize.x * (-50.0/100))+91;
                yOffset = int(guiSize.y * (0.0/100))+50;
                layer = 1;
                break;
            case 672:
                xOffset = int(guiSize.x * (-50.0/100))+90;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 2;
                break;
            case 673:
                xOffset = int(guiSize.x * (-50.0/100))+101;
                yOffset = int(guiSize.y * (0.0/100))+49;
                layer = 3;
                break;
            case 674:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+85;
                layer = 1;
                outlined = true;
                break;
            case 675:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+88;
                layer = 1;
                outlined = true;
                break;
            case 676:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+75;
                layer = 1;
                outlined = true;
                break;
            case 677:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+78;
                layer = 1;
                outlined = true;
                break;
            case 678:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+65;
                layer = 1;
                outlined = true;
                break;
            case 679:
                xOffset = int(guiSize.x * (-50.0/100));
                yOffset = int(guiSize.y * (0.0/100))+68;
                layer = 1;
                outlined = true;
                break;
            case 680:
                xOffset = int(guiSize.x * (-100.0/100))+20;
                yOffset = int(guiSize.y * (100.0/100))-20;
                layer = 1;
                break;
            case 681:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-22;
                layer = 2;
                outlined = true;
                break;
            case 682:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-19;
                layer = 2;
                outlined = true;
                break;
            case 683:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-22;
                layer = 3;
                outlined = true;
                break;
            case 684:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-19;
                layer = 3;
                outlined = true;
                break;
            case 685:
                xOffset = int(guiSize.x * (-100.0/100))-66;
                yOffset = int(guiSize.y * (100.0/100))-22;
                layer = 4;
                break;
            case 686:
                xOffset = int(guiSize.x * (-100.0/100))+20;
                yOffset = int(guiSize.y * (100.0/100))-40;
                layer = 1;
                break;
            case 687:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-42;
                layer = 2;
                outlined = true;
                break;
            case 688:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-39;
                layer = 2;
                outlined = true;
                break;
            case 689:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-42;
                layer = 3;
                outlined = true;
                break;
            case 690:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-39;
                layer = 3;
                outlined = true;
                break;
            case 691:
                xOffset = int(guiSize.x * (-100.0/100))-66;
                yOffset = int(guiSize.y * (100.0/100))-42;
                layer = 4;
                break;
            case 692:
                xOffset = int(guiSize.x * (-100.0/100))+20;
                yOffset = int(guiSize.y * (100.0/100))-60;
                layer = 1;
                break;
            case 693:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-62;
                layer = 2;
                outlined = true;
                break;
            case 694:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-59;
                layer = 2;
                outlined = true;
                break;
            case 695:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-62;
                layer = 3;
                outlined = true;
                break;
            case 696:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-59;
                layer = 3;
                outlined = true;
                break;
            case 697:
                xOffset = int(guiSize.x * (-100.0/100))-66;
                yOffset = int(guiSize.y * (100.0/100))-62;
                layer = 4;
                break;
            case 698:
                xOffset = int(guiSize.x * (-100.0/100))+20;
                yOffset = int(guiSize.y * (100.0/100))-80;
                layer = 1;
                break;
            case 699:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-82;
                layer = 2;
                outlined = true;
                break;
            case 700:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-79;
                layer = 2;
                outlined = true;
                break;
            case 701:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-82;
                layer = 3;
                outlined = true;
                break;
            case 702:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-79;
                layer = 3;
                outlined = true;
                break;
            case 703:
                xOffset = int(guiSize.x * (-100.0/100))-66;
                yOffset = int(guiSize.y * (100.0/100))-82;
                layer = 4;
                break;
            case 704:
                xOffset = int(guiSize.x * (-100.0/100))+20;
                yOffset = int(guiSize.y * (100.0/100))-100;
                layer = 1;
                break;
            case 705:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-102;
                layer = 2;
                outlined = true;
                break;
            case 706:
                xOffset = int(guiSize.x * (-100.0/100))-80;
                yOffset = int(guiSize.y * (100.0/100))-99;
                layer = 2;
                outlined = true;
                break;
            case 707:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-102;
                layer = 3;
                outlined = true;
                break;
            case 708:
                xOffset = int(guiSize.x * (-100.0/100))-60;
                yOffset = int(guiSize.y * (100.0/100))-99;
                layer = 3;
                outlined = true;
                break;
            case 709:
                xOffset = int(guiSize.x * (-100.0/100))-66;
                yOffset = int(guiSize.y * (100.0/100))-102;
                layer = 4;
                break;
        }
        if (true) {
            pos.y -= (happyId * 1000.0) + 500.0 - 64.0;
            pos.x -= guiSize.x * 0.5;
            pos -= vec3(xOffset, yOffset, 0.0);
            pos.z += float(layer);
        }
    }

#endif
    gl_Position = ProjMat * ModelViewMat * vec4(pos, 1.0);

#if !defined(IS_GUI) && !defined(IS_SEE_THROUGH)
    sphericalVertexDistance = fog_spherical_distance(Position);
    cylindricalVertexDistance = fog_cylindrical_distance(Position);
    vertexColor = Color * sample_lightmap(Sampler2, UV2);
#else
    vertexColor = Color;
#endif
    texCoord0 = UV0;

    vec4 vcolor = vertexColor;
    #moj_import <minecraft:minimap/vertex_main.glsl>
    minimapCarrierPosition = Position;
}
