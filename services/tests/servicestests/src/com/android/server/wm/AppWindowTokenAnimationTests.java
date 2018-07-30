/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

package com.android.server.wm;

import static android.app.WindowConfiguration.ACTIVITY_TYPE_STANDARD;
import static android.app.WindowConfiguration.WINDOWING_MODE_FULLSCREEN;
import static android.view.SurfaceControl.Transaction;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import android.support.test.filters.SmallTest;
import android.support.test.runner.AndroidJUnit4;
import android.view.SurfaceControl;

import com.android.server.wm.WindowTestUtils.TestAppWindowToken;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Animation related tests for the {@link AppWindowToken} class.
 *
 * Build/Install/Run:
 * atest FrameworksServicesTests:com.android.server.wm.AppWindowTokenAnimationTests
 */
@SmallTest
@RunWith(AndroidJUnit4.class)
public class AppWindowTokenAnimationTests extends WindowTestsBase {

    private TestAppWindowToken mToken;

    @Mock
    private Transaction mTransaction;
    @Mock
    private AnimationAdapter mSpec;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        MockitoAnnotations.initMocks(this);

        mToken = createTestAppWindowToken(mDisplayContent, WINDOWING_MODE_FULLSCREEN,
                ACTIVITY_TYPE_STANDARD);
        mToken.setPendingTransaction(mTransaction);
    }

    @Test
    public void clipAfterAnim_boundsLayerIsCreated() throws Exception {
        mToken.mNeedsAnimationBoundsLayer = true;

        mToken.mSurfaceAnimator.startAnimation(mTransaction, mSpec, true /* hidden */);
        verify(mTransaction).reparent(eq(mToken.getSurfaceControl()),
                eq(mToken.mSurfaceAnimator.mLeash.getHandle()));
        verify(mTransaction).reparent(eq(mToken.mSurfaceAnimator.mLeash),
                eq(mToken.mAnimationBoundsLayer.getHandle()));
    }

    @Test
    public void clipAfterAnim_boundsLayerIsDestroyed() throws Exception {
        mToken.mNeedsAnimationBoundsLayer = true;
        mToken.mSurfaceAnimator.startAnimation(mTransaction, mSpec, true /* hidden */);
        final SurfaceControl leash = mToken.mSurfaceAnimator.mLeash;
        final SurfaceControl animationBoundsLayer = mToken.mAnimationBoundsLayer;
        final ArgumentCaptor<SurfaceAnimator.OnAnimationFinishedCallback> callbackCaptor =
                ArgumentCaptor.forClass(
                        SurfaceAnimator.OnAnimationFinishedCallback.class);
        verify(mSpec).startAnimation(any(), any(), callbackCaptor.capture());

        callbackCaptor.getValue().onAnimationFinished(mSpec);
        verify(mTransaction).destroy(eq(leash));
        verify(mTransaction).destroy(eq(animationBoundsLayer));
        assertThat(mToken.mNeedsAnimationBoundsLayer).isFalse();
    }

    @Test
    public void clipAfterAnimCancelled_boundsLayerIsDestroyed() throws Exception {
        mToken.mNeedsAnimationBoundsLayer = true;
        mToken.mSurfaceAnimator.startAnimation(mTransaction, mSpec, true /* hidden */);
        final SurfaceControl leash = mToken.mSurfaceAnimator.mLeash;
        final SurfaceControl animationBoundsLayer = mToken.mAnimationBoundsLayer;

        mToken.mSurfaceAnimator.cancelAnimation();
        verify(mTransaction).destroy(eq(leash));
        verify(mTransaction).destroy(eq(animationBoundsLayer));
        assertThat(mToken.mNeedsAnimationBoundsLayer).isFalse();
    }

    @Test
    public void clipNoneAnim_boundsLayerIsNotCreated() throws Exception {
        mToken.mNeedsAnimationBoundsLayer = false;

        mToken.mSurfaceAnimator.startAnimation(mTransaction, mSpec, true /* hidden */);
        verify(mTransaction).reparent(eq(mToken.getSurfaceControl()),
                eq(mToken.mSurfaceAnimator.mLeash.getHandle()));
        assertThat(mToken.mAnimationBoundsLayer).isNull();
    }
}
