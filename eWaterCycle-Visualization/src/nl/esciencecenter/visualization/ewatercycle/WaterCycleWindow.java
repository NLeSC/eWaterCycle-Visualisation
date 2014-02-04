package nl.esciencecenter.visualization.ewatercycle;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;

import nl.esciencecenter.neon.datastructures.FrameBufferObject;
import nl.esciencecenter.neon.datastructures.IntPixelBufferObject;
import nl.esciencecenter.neon.exceptions.UninitializedException;
import nl.esciencecenter.neon.math.Color4;
import nl.esciencecenter.neon.math.Float2Vector;
import nl.esciencecenter.neon.math.Float3Vector;
import nl.esciencecenter.neon.math.Float4Matrix;
import nl.esciencecenter.neon.math.Float4Vector;
import nl.esciencecenter.neon.math.FloatMatrixMath;
import nl.esciencecenter.neon.math.Point4;
import nl.esciencecenter.neon.models.GeoSphere;
import nl.esciencecenter.neon.models.Model;
import nl.esciencecenter.neon.models.Quad;
import nl.esciencecenter.neon.shaders.ShaderProgram;
import nl.esciencecenter.neon.shaders.ShaderProgramLoader;
import nl.esciencecenter.neon.text.MultiColorText;
import nl.esciencecenter.neon.text.jogampexperimental.Font;
import nl.esciencecenter.neon.text.jogampexperimental.FontFactory;
import nl.esciencecenter.neon.textures.Texture2D;
import nl.esciencecenter.visualization.ewatercycle.data.ImauTimedPlayer;
import nl.esciencecenter.visualization.ewatercycle.data.SurfaceTextureDescription;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WaterCycleWindow implements GLEventListener {
    private final static Logger logger = LoggerFactory.getLogger(WaterCycleWindow.class);
    private final WaterCycleSettings settings = WaterCycleSettings.getInstance();

    private Quad fsq;

    protected final ShaderProgramLoader loader;
    protected final WaterCycleInputHandler inputHandler;

    private ShaderProgram shaderProgram_Sphere, shaderProgram_Legend, shaderProgram_Atmosphere,
            shaderProgram_GaussianBlur, shaderProgram_FlattenLayers, shaderProgram_PostProcess, shaderProgram_Text;

    private Model sphereModel, legendModel, atmModel;

    private FrameBufferObject atmosphereFrameBufferObject, hudTextFrameBufferObject, legendTextureFrameBufferObject,
            sphereTextureFrameBufferObject;

    private IntPixelBufferObject finalPBO;

    private final BufferedImage currentImage = null;

    private final int fontSize = 42;

    private boolean reshaped = false;

    private SurfaceTextureDescription[] cachedTextureDescriptions;
    private FrameBufferObject[] cachedFrameBufferObjects;
    private final MultiColorText[] varNames;
    private final MultiColorText[] legendTextsMin;
    private final MultiColorText[] legendTextsMax;
    private final MultiColorText[] dates;
    private final MultiColorText[] dataSets;
    //
    // private MultiColorText testText, varNameText, datasetText, dateText,
    // legendTextMin, legendTextMax;

    private int cachedScreens = 9;

    private ImauTimedPlayer timer;
    private float aspect;

    protected int fontSet = FontFactory.UBUNTU;
    protected Font font;

    private final float radius = 1.0f;
    private final float ftheta = 0.0f;
    private final float phi = 0.0f;

    private final float fovy = 45.0f;
    private final float zNear = 0.1f;
    private final float zFar = 3000.0f;

    private final Texture2D[] cachedSurfaceTextures;
    private final Texture2D[] cachedLegendTextures;

    // Height and width of the drawable area. We extract this from the opengl
    // instance in the reshape method every time it is changed, but set it in
    // the init method initially. The default values are defined by the settings
    // class.
    private int canvasWidth, canvasHeight;

    // Variables needed to calculate the viewpoint and camera angle.
    final Point4 eye = new Point4((float) (radius * Math.sin(ftheta) * Math.cos(phi)), (float) (radius
            * Math.sin(ftheta) * Math.sin(phi)), (float) (radius * Math.cos(ftheta)));
    final Point4 at = new Point4(0.0f, 0.0f, 0.0f);
    final Float4Vector up = new Float4Vector(0.0f, 1.0f, 0.0f, 0.0f);

    public WaterCycleWindow(WaterCycleInputHandler inputHandler) {
        this.loader = new ShaderProgramLoader();
        this.inputHandler = inputHandler;
        this.font = FontFactory.get(fontSet).getDefault();

        varNames = new MultiColorText[cachedScreens];
        legendTextsMin = new MultiColorText[cachedScreens];
        legendTextsMax = new MultiColorText[cachedScreens];
        dates = new MultiColorText[cachedScreens];
        dataSets = new MultiColorText[cachedScreens];

        cachedSurfaceTextures = new Texture2D[cachedScreens];
        cachedLegendTextures = new Texture2D[cachedScreens];
    }

    public static void contextOn(GLAutoDrawable drawable) {
        try {
            final int status = drawable.getContext().makeCurrent();
            if ((status != GLContext.CONTEXT_CURRENT) && (status != GLContext.CONTEXT_CURRENT_NEW)) {
                System.err.println("Error swapping context to onscreen.");
            }
        } catch (final GLException e) {
            System.err.println("Exception while swapping context to onscreen.");
            e.printStackTrace();
        }
    }

    public static void contextOff(GLAutoDrawable drawable) {
        // Release the context.
        try {
            drawable.getContext().release();
        } catch (final GLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init(GLAutoDrawable drawable) {
        // Get the Opengl context from the drawable, and make it current, so
        // we can see it and draw on it. I've never seen this fail, but there is
        // error checking anyway.
        contextOn(drawable);

        // Once we have the context current, we can extract the OpenGL instance
        // from it. We have defined a OpenGL 3.0 instance in the
        // neonNewtWindow by adding the line
        // glp = GLProfile.get(GLProfile.GL3);
        // Therefore, we extract a GL3 instance, so we cannot make any
        // unfortunate mistakes (calls to methods that are undefined for this
        // version).
        final GL3 gl = GLContext.getCurrentGL().getGL3();

        // set the canvas size and aspect ratio in the global variables.
        canvasWidth = GLContext.getCurrent().getGLDrawable().getWidth();
        canvasHeight = GLContext.getCurrent().getGLDrawable().getHeight();
        aspect = (float) canvasWidth / (float) canvasHeight;

        // Enable Anti-Aliasing (smoothing of jagged edges on the edges of
        // objects).
        gl.glEnable(GL3.GL_LINE_SMOOTH);
        gl.glHint(GL3.GL_LINE_SMOOTH_HINT, GL3.GL_NICEST);
        gl.glEnable(GL3.GL_POLYGON_SMOOTH);
        gl.glHint(GL3.GL_POLYGON_SMOOTH_HINT, GL3.GL_NICEST);

        // Enable Depth testing (Render only those objects that are not obscured
        // by other objects).
        gl.glEnable(GL3.GL_DEPTH_TEST);
        gl.glDepthFunc(GL3.GL_LEQUAL);
        gl.glClearDepth(1.0f);

        // Enable Culling (render only the camera-facing sides of objects).
        gl.glEnable(GL3.GL_CULL_FACE);
        gl.glCullFace(GL3.GL_BACK);

        // Enable Blending (needed for both Transparency and Anti-Aliasing)
        gl.glBlendFunc(GL3.GL_SRC_ALPHA, GL3.GL_ONE_MINUS_SRC_ALPHA);
        gl.glEnable(GL3.GL_BLEND);

        // Enable Vertical Sync
        gl.setSwapInterval(1);

        // Set black background
        gl.glClearColor(0f, 0f, 0f, 0f);

        // Enable programmatic setting of point size, for rendering points (not
        // needed for this example application).
        gl.glEnable(GL3.GL_PROGRAM_POINT_SIZE);

        // worldTex = new ImageTexture("textures/4_no_ice_clouds_mts_8k.jpg",
        // 4096, 0, GL3.GL_TEXTURE2);
        // bumpTex = new BumpTexture("textures/elev_bump_8k.jpg", 4096, 0,
        // GL3.GL_TEXTURE3);
        // worldTex.init(gl);
        // bumpTex.init(gl);

        // atmModel = new GeoSphere(new Material(atmosphereColor,
        // atmosphereColor, atmosphereColor), 50, 50, 55f, false);

        try {
            shaderProgram_Sphere = loader.createProgram(gl, "shaderProgram_Sphere", new File("shaders/vs_texture.vp"),
                    new File("shaders/fs_texture.fp"));

            shaderProgram_Legend = loader.createProgram(gl, "shaderProgram_Legend", new File("shaders/vs_texture.vp"),
                    new File("shaders/fs_texture.fp"));

            shaderProgram_Text = loader.createProgram(gl, "shaderProgram_Text", new File(
                    "shaders/vs_multiColorTextShader.vp"), new File("shaders/fs_multiColorTextShader.fp"));

            shaderProgram_Atmosphere = loader.createProgram(gl, "shaderProgram_Atmosphere", new File(
                    "shaders/vs_atmosphere.vp"), new File("shaders/fs_atmosphere.fp"));

            shaderProgram_GaussianBlur = loader.createProgram(gl, "shaderProgram_GaussianBlur", new File(
                    "shaders/vs_postprocess.vp"), new File("shaders/fs_gaussian_blur.fp"));

            shaderProgram_PostProcess = loader.createProgram(gl, "shaderProgram_PostProcess", new File(
                    "shaders/vs_postprocess.vp"), new File("shaders/fs_postprocess.fp"));

            shaderProgram_FlattenLayers = loader.createProgram(gl, "shaderProgram_FlattenLayers", new File(
                    "shaders/vs_flatten3.vp"), new File("shaders/fs_flatten3.fp"));
        } catch (final Exception e) {
            // If compilation fails, we will output the error message and quit
            // the application.
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }

        fsq = new Quad(2, 2, new Float3Vector(0, 0, 0.1f));
        fsq.init(gl);

        // sphereModel = new GeoSphereCut(Material.random(), 120, 120, 50f,
        // false);
        sphereModel = new GeoSphere(60, 60, 50f, false);
        sphereModel.init(gl);

        // cutModel = new GeoSphereCutEdge(Material.random(), 120, 50f);
        // cutModel.init(gl);

        legendModel = new Quad(1.5f, .1f, new Float3Vector(1, 0, 0.1f));
        legendModel.init(gl);

        atmModel = new GeoSphere(60, 60, 53f, false);
        atmModel.init(gl);

        // Material textMaterial = new Material(Color4.white, Color4.white,
        // Color4.white);

        // testText = new MultiColorText(gl, font, "test1", Color4.white,
        // fontSize);
        // testText.init(gl);

        initDatastores(gl);

        inputHandler.setViewDist(-130f);

        for (int i = 0; i < cachedScreens; i++) {
            varNames[i] = new MultiColorText(gl, font, "test1", Color4.WHITE, fontSize);
            varNames[i].init(gl);

            legendTextsMin[i] = new MultiColorText(gl, font, "test1", Color4.WHITE, fontSize);
            legendTextsMin[i].init(gl);

            legendTextsMax[i] = new MultiColorText(gl, font, "test1", Color4.WHITE, fontSize);
            legendTextsMax[i].init(gl);

            dates[i] = new MultiColorText(gl, font, "test1", Color4.WHITE, fontSize);
            dates[i].init(gl);

            dataSets[i] = new MultiColorText(gl, font, "test1", Color4.WHITE, fontSize);
            dataSets[i].init(gl);

        }

        // Here we define a PixelBufferObject, which is used for getting
        // screenshots.
        finalPBO = new IntPixelBufferObject(canvasWidth, canvasHeight);
        finalPBO.init(gl);

        contextOff(drawable);
    }

    @Override
    public void display(GLAutoDrawable drawable) {
        // Get the Opengl context from the drawable, and make it current, so
        // we can see it and draw on it. I've never seen this fail, but there is
        // error checking anyway.
        contextOn(drawable);

        // Once we have the context current, we can extract the OpenGL instance
        // from it. We have defined a OpenGL 3.0 instance in the
        // neonNewtWindow by adding the line
        // glp = GLProfile.get(GLProfile.GL3);
        // Therefore, we extract a GL3 instance, so we cannot make any
        // unfortunate mistakes (calls to methods that are undefined for this
        // version).
        final GL3 gl = GLContext.getCurrentGL().getGL3();

        // Check if the shape is still correct (if we have missed a reshape
        // event this might not be the case).
        if (canvasWidth != GLContext.getCurrent().getGLDrawable().getWidth()
                || canvasHeight != GLContext.getCurrent().getGLDrawable().getHeight()) {
            doReshape(gl);
        }

        // First, we clear the buffer to start with a clean slate to draw on.
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        // Construct a modelview matrix out of camera viewpoint and angle.
        Float4Matrix modelViewMatrix = FloatMatrixMath.lookAt(eye, at, up);

        // Translate the camera backwards according to the inputhandler's view
        // distance setting.
        modelViewMatrix = modelViewMatrix.mul(FloatMatrixMath.translate(new Float3Vector(0f, 0f, inputHandler
                .getViewDist())));

        // Rotate tha camera according to the rotation angles defined in the
        // inputhandler.
        modelViewMatrix = modelViewMatrix.mul(FloatMatrixMath.rotationX(inputHandler.getRotation().getX()));
        modelViewMatrix = modelViewMatrix.mul(FloatMatrixMath.rotationY(inputHandler.getRotation().getY()));
        modelViewMatrix = modelViewMatrix.mul(FloatMatrixMath.rotationZ(inputHandler.getRotation().getZ()));

        ImauTimedPlayer timer = WaterCyclePanel.getTimer();
        if (timer.isInitialized()) {
            this.timer = timer;

            Float2Vector clickCoords = null;

            int currentScreens = settings.getNumScreensRows() * settings.getNumScreensCols();
            if (currentScreens != cachedScreens) {
                initDatastores(gl);
                timer.reinitializeDatastores();
            }

            displayContext(gl, timer, clickCoords);
        }

        reshaped = false;

        contextOff(drawable);
    }

    private void displayContext(GL3 gl, ImauTimedPlayer timer, Float2Vector clickCoords) {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        final Point4 eye = new Point4((float) (radius * Math.sin(ftheta) * Math.cos(phi)), (float) (radius
                * Math.sin(ftheta) * Math.sin(phi)), (float) (radius * Math.cos(ftheta)));
        final Point4 at = new Point4(0.0f, 0.0f, 0.0f);
        final Float4Vector up = new Float4Vector(0.0f, 1.0f, 0.0f, 0.0f);

        Float4Matrix mv = FloatMatrixMath.lookAt(eye, at, up);
        mv = mv.mul(FloatMatrixMath.translate(new Float3Vector(0f, 0f, inputHandler.getViewDist())));
        mv = mv.mul(FloatMatrixMath.rotationX(inputHandler.getRotation().getX()));
        mv = mv.mul(FloatMatrixMath.rotationY(inputHandler.getRotation().getY()));

        drawAtmosphere(gl, mv, atmosphereFrameBufferObject);
        // blur(gl, atmosphereFrameBufferObject, fsq, 1, 2, 4);

        SurfaceTextureDescription currentDesc;

        for (int i = 0; i < cachedScreens; i++) {
            currentDesc = settings.getSurfaceDescription(i);
            if (currentDesc != null && !currentDesc.equals(cachedTextureDescriptions[i]) || reshaped) {
                logger.debug(currentDesc.toString());

                timer.getEfficientTextureStorage().requestNewConfiguration(i, currentDesc);

                String variableName = currentDesc.getVarName();
                String fancyName = variableName;
                // String units = timer.getVariableUnits(variableName);
                // fancyName += " in " + units;
                varNames[i].setString(gl, fancyName, Color4.WHITE, fontSize);

                String min, max;
                if (currentDesc.isDiff()) {
                    min = Float.toString(settings.getCurrentVarDiffMin(currentDesc.getVarName()));
                    max = Float.toString(settings.getCurrentVarDiffMax(currentDesc.getVarName()));
                } else {
                    min = Float.toString(settings.getCurrentVarMin(currentDesc.getVarName()));
                    max = Float.toString(settings.getCurrentVarMax(currentDesc.getVarName()));
                }
                dates[i].setString(gl, settings.getFancyDate(currentDesc.getFrameNumber()), Color4.WHITE, fontSize);
                dataSets[i].setString(gl, currentDesc.verbalizeDataMode(), Color4.WHITE, fontSize);
                legendTextsMin[i].setString(gl, min, Color4.WHITE, fontSize);
                legendTextsMax[i].setString(gl, max, Color4.WHITE, fontSize);

                cachedTextureDescriptions[i] = currentDesc;

                // Texture2D surfaceBuffer =
                // timer.getEfficientTextureStorage().getSurfaceImage(i);
                // Texture2D legendBuffer =
                // timer.getEfficientTextureStorage().getLegendImage(i);
                //
                // if (surfaceBuffer != null) {
                // if (cachedSurfaceTextures[i] != null) {
                // cachedSurfaceTextures[i].delete(gl);
                // }
                // cachedSurfaceTextures[i] = new
                // ByteBufferTexture(GL3.GL_TEXTURE4, surfaceBuffer,
                // timer.getImageWidth(), timer.getImageHeight());
                // cachedSurfaceTextures[i].init(gl);
                // }
                //
                // if (legendBuffer != null) {
                // if (cachedLegendTextures[i] != null) {
                // cachedLegendTextures[i].delete(gl);
                // }
                // cachedLegendTextures[i] = new
                // ByteBufferTexture(GL3.GL_TEXTURE5, legendBuffer, 1, 500);
                // cachedLegendTextures[i].init(gl);
                // }

                cachedSurfaceTextures[i] = timer.getEfficientTextureStorage().getSurfaceImage(i);
                cachedLegendTextures[i] = timer.getEfficientTextureStorage().getLegendImage(i);

                cachedSurfaceTextures[i].init(gl);
                cachedLegendTextures[i].init(gl);
            }

            if (cachedLegendTextures[i] != null && cachedSurfaceTextures[i] != null) {
                drawSingleWindow(gl, mv, i, cachedLegendTextures[i], cachedSurfaceTextures[i],
                        cachedFrameBufferObjects[i], clickCoords);
            }
        }

        // logger.debug("Tiling windows");
        renderTexturesToScreen(gl);
    }

    private void drawSingleWindow(final GL3 gl, Float4Matrix mv, int windowIndex, Texture2D legend, Texture2D globe,
            FrameBufferObject target, Float2Vector clickCoords) {
        // logger.debug("Drawing Text");
        drawHUDText(gl, windowIndex, hudTextFrameBufferObject);

        // logger.debug("Drawing HUD");
        drawHUDLegend(gl, legend, legendTextureFrameBufferObject);

        // logger.debug("Drawing Sphere");
        drawSphere(gl, mv, globe, sphereTextureFrameBufferObject, clickCoords);

        // logger.debug("Flattening Layers");
        flattenLayers(gl, hudTextFrameBufferObject, legendTextureFrameBufferObject, sphereTextureFrameBufferObject,
                atmosphereFrameBufferObject, target);
    }

    private void drawHUDText(GL3 gl, int windowIndex, FrameBufferObject target) {
        // testText.setString(gl, "test2", Color4.white, fontSize);

        // String randomString = "Random: " + Math.random();
        // testText.setString(gl, randomString, Color4.white, fontSize);

        try {
            target.bind(gl);
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

            // testText.draw(gl, shaderProgram_Text, canvasWidth, canvasHeight,
            // 30f, 30f);

            // Draw text
            int textLength = varNames[windowIndex].toString().length() * fontSize;

            varNames[windowIndex].drawHudRelative(gl, shaderProgram_Text, canvasWidth, canvasHeight, 2 * canvasWidth
                    - textLength - 150, 40);

            // textLength = dataSets[windowIndex].toString().length() *
            // fontSize;
            // dataSets[windowIndex].draw(gl, shaderProgram_Text, canvasWidth,
            // canvasHeight, 10, 1.9f * canvasHeight);

            textLength = dates[windowIndex].toString().length() * fontSize;
            dates[windowIndex].drawHudRelative(gl, shaderProgram_Text, canvasWidth, canvasHeight, 10, 40);

            textLength = legendTextsMin[windowIndex].toString().length() * fontSize;
            legendTextsMin[windowIndex].drawHudRelative(gl, shaderProgram_Text, canvasWidth, canvasHeight, 2
                    * canvasWidth - textLength - 100, .2f * canvasHeight);

            textLength = legendTextsMax[windowIndex].toString().length() * fontSize;
            legendTextsMax[windowIndex].drawHudRelative(gl, shaderProgram_Text, canvasWidth, canvasHeight, 2
                    * canvasWidth - textLength - 100, 1.75f * canvasHeight);

            target.unBind(gl);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }
    }

    private void drawHUDLegend(GL3 gl, Texture2D legendTexture, FrameBufferObject target) {
        try {
            target.bind(gl);
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

            // Draw legend texture
            legendTexture.use(gl);
            shaderProgram_Legend.setUniform("texture_map", legendTexture.getMultitexNumber());
            shaderProgram_Legend.setUniformMatrix("MVMatrix", new Float4Matrix());
            shaderProgram_Legend.setUniformMatrix("PMatrix", new Float4Matrix());

            shaderProgram_Legend.use(gl);
            legendModel.draw(gl, shaderProgram_Legend);

            target.unBind(gl);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }
    }

    private void drawSphere(GL3 gl, Float4Matrix mv, Texture2D surfaceTexture, FrameBufferObject target,
            Float2Vector clickCoords) {
        try {
            target.bind(gl);
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

            final Float4Matrix p = FloatMatrixMath.perspective(fovy, aspect, zNear, zFar);

            shaderProgram_Sphere.setUniformMatrix("MVMatrix", new Float4Matrix(mv));
            shaderProgram_Sphere.setUniformMatrix("PMatrix", p);

            surfaceTexture.use(gl);

            shaderProgram_Sphere.setUniform("texture_map", surfaceTexture.getMultitexNumber());

            shaderProgram_Sphere.use(gl);
            sphereModel.draw(gl, shaderProgram_Sphere);

            target.unBind(gl);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }
    }

    private void drawAtmosphere(GL3 gl, Float4Matrix mv, FrameBufferObject target) {
        try {
            target.bind(gl);
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

            final Float4Matrix p = FloatMatrixMath.perspective(fovy, aspect, zNear, zFar);
            shaderProgram_Atmosphere.setUniformMatrix("MVMatrix", new Float4Matrix(mv));
            shaderProgram_Atmosphere.setUniformMatrix("PMatrix", p);
            shaderProgram_Atmosphere.setUniformMatrix("NormalMatrix", FloatMatrixMath.getNormalMatrix(mv));

            shaderProgram_Atmosphere.use(gl);
            atmModel.draw(gl, shaderProgram_Atmosphere);

            target.unBind(gl);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }
    }

    private void flattenLayers(GL3 gl, FrameBufferObject hudTextFrameBufferObject,
            FrameBufferObject hudLegendFrameBufferObject, FrameBufferObject sphereTextureFrameBufferObject,
            FrameBufferObject atmosphereFrameBufferObject, FrameBufferObject target) {
        try {
            target.bind(gl);
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

            shaderProgram_FlattenLayers
                    .setUniform("textTex", hudTextFrameBufferObject.getTexture().getMultitexNumber());
            shaderProgram_FlattenLayers.setUniform("legendTex", hudLegendFrameBufferObject.getTexture()
                    .getMultitexNumber());
            shaderProgram_FlattenLayers.setUniform("dataTex", sphereTextureFrameBufferObject.getTexture()
                    .getMultitexNumber());
            shaderProgram_FlattenLayers.setUniform("atmosphereTex", atmosphereFrameBufferObject.getTexture()
                    .getMultitexNumber());

            shaderProgram_FlattenLayers.setUniformMatrix("MVMatrix", new Float4Matrix());
            shaderProgram_FlattenLayers.setUniformMatrix("PMatrix", new Float4Matrix());

            shaderProgram_FlattenLayers.setUniform("scrWidth", canvasWidth);
            shaderProgram_FlattenLayers.setUniform("scrHeight", canvasHeight);

            shaderProgram_FlattenLayers.use(gl);
            fsq.draw(gl, shaderProgram_FlattenLayers);

            target.unBind(gl);
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }
    }

    private void blur(GL3 gl, FrameBufferObject target, Quad fullScreenQuad, int passes, int blurType, float blurSize) {
        shaderProgram_GaussianBlur.setUniform("Texture", target.getTexture().getMultitexNumber());

        shaderProgram_GaussianBlur.setUniformMatrix("PMatrix", new Float4Matrix());
        shaderProgram_GaussianBlur.setUniformMatrix("MVMatrix", new Float4Matrix());

        shaderProgram_GaussianBlur.setUniform("scrWidth", target.getTexture().getWidth());
        shaderProgram_GaussianBlur.setUniform("scrHeight", target.getTexture().getHeight());

        shaderProgram_GaussianBlur.setUniform("blurDirection", 0);
        shaderProgram_GaussianBlur.setUniform("blurSize", blurSize);
        shaderProgram_GaussianBlur.setUniform("blurType", blurType);

        shaderProgram_GaussianBlur.setUniform("Sigma", 0f);
        shaderProgram_GaussianBlur.setUniform("NumPixelsPerSide", 0f);
        shaderProgram_GaussianBlur.setUniform("Alpha", 1f);

        try {
            target.bind(gl);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            for (int i = 0; i < passes; i++) {
                shaderProgram_GaussianBlur.setUniform("blurDirection", 0);

                shaderProgram_GaussianBlur.use(gl);
                fullScreenQuad.draw(gl, shaderProgram_GaussianBlur);

                shaderProgram_GaussianBlur.setUniform("blurDirection", 1);

                shaderProgram_GaussianBlur.use(gl);
                fullScreenQuad.draw(gl, shaderProgram_GaussianBlur);
            }

            target.unBind(gl);
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }
    }

    public void renderTexturesToScreen(GL3 gl) {
        try {
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

            for (int i = 0; i < cachedScreens; i++) {
                shaderProgram_PostProcess.setUniform("sphereTexture_" + i, cachedFrameBufferObjects[i].getTexture()
                        .getMultitexNumber());
            }

            shaderProgram_PostProcess.setUniformMatrix("MVMatrix", new Float4Matrix());
            shaderProgram_PostProcess.setUniformMatrix("PMatrix", new Float4Matrix());

            shaderProgram_PostProcess.setUniform("scrWidth", canvasWidth);
            shaderProgram_PostProcess.setUniform("scrHeight", canvasHeight);

            int selection = settings.getWindowSelection();

            shaderProgram_PostProcess.setUniform("divs_x", settings.getNumScreensCols());
            shaderProgram_PostProcess.setUniform("divs_y", settings.getNumScreensRows());
            shaderProgram_PostProcess.setUniform("selection", selection);

            shaderProgram_PostProcess.use(gl);
            fsq.draw(gl, shaderProgram_PostProcess);
        } catch (final UninitializedException e) {
            e.printStackTrace();
        }
    }

    private void initDatastores(GL3 gl) {
        cachedScreens = settings.getNumScreensRows() * settings.getNumScreensCols();

        cachedTextureDescriptions = new SurfaceTextureDescription[cachedScreens];
        // varNames = new MultiColorText[cachedScreens];
        // legendTextsMin = new MultiColorText[cachedScreens];
        // legendTextsMax = new MultiColorText[cachedScreens];
        // dates = new MultiColorText[cachedScreens];
        // dataSets = new MultiColorText[cachedScreens];

        if (atmosphereFrameBufferObject != null) {
            atmosphereFrameBufferObject.delete(gl);
        }
        if (hudTextFrameBufferObject != null) {
            hudTextFrameBufferObject.delete(gl);
        }
        if (legendTextureFrameBufferObject != null) {
            legendTextureFrameBufferObject.delete(gl);
        }
        if (sphereTextureFrameBufferObject != null) {
            sphereTextureFrameBufferObject.delete(gl);
        }

        logger.debug("FrameBufferObject initialization with width: " + canvasWidth + ", height: " + canvasHeight);

        atmosphereFrameBufferObject = new FrameBufferObject(canvasWidth, canvasHeight, GL.GL_TEXTURE0);
        hudTextFrameBufferObject = new FrameBufferObject(canvasWidth, canvasHeight, GL.GL_TEXTURE1);
        legendTextureFrameBufferObject = new FrameBufferObject(canvasWidth, canvasHeight, GL.GL_TEXTURE2);
        sphereTextureFrameBufferObject = new FrameBufferObject(canvasWidth, canvasHeight, GL.GL_TEXTURE3);

        atmosphereFrameBufferObject.init(gl);
        hudTextFrameBufferObject.init(gl);
        legendTextureFrameBufferObject.init(gl);
        sphereTextureFrameBufferObject.init(gl);

        if (cachedFrameBufferObjects == null || cachedScreens != cachedFrameBufferObjects.length) {
            cachedFrameBufferObjects = new FrameBufferObject[cachedScreens];
        }

        logger.debug("CACHED SCREENS: " + cachedScreens);

        for (int i = 0; i < cachedScreens; i++) {
            cachedTextureDescriptions[i] = settings.getSurfaceDescription(i);

            if (cachedFrameBufferObjects[i] != null) {
                cachedFrameBufferObjects[i].delete(gl);
            }
            cachedFrameBufferObjects[i] = new FrameBufferObject(canvasWidth, canvasHeight, (GL.GL_TEXTURE6 + i));

            logger.debug("Cached FrameBufferObject init nr: " + i);
            cachedFrameBufferObjects[i].init(gl);

            // String text = "garbled!";

            // varNames[i] = new MultiColorText(gl, font, text, Color4.white,
            // fontSize);
            // legendTextsMin[i] = new MultiColorText(gl, font, text,
            // Color4.white, fontSize);
            // legendTextsMin[i] = new MultiColorText(gl, font, text,
            // Color4.white, fontSize);
            // legendTextsMax[i] = new MultiColorText(gl, font, text,
            // Color4.white, fontSize);
            // dates[i] = new MultiColorText(gl, font, text, Color4.white,
            // fontSize);
            // dataSets[i] = new MultiColorText(gl, font, text, Color4.white,
            // fontSize);
            //
            // varNames[i].init(gl);
            // legendTextsMin[i].init(gl);
            // legendTextsMax[i].init(gl);
            // dates[i].init(gl);
            // dataSets[i].init(gl);
        }

        // try {
        // if (shaderProgram_PostProcess != null) {
        // shaderProgram_PostProcess.delete(gl);
        // shaderProgram_PostProcess = loader.createProgram(gl,
        // "postprocess", new File("shaders/vs_postprocess.vp"),
        // new File("shaders/fs_postprocess.fp"));
        // // PostprocShaderCreator.generateShaderText(
        // // settings.getNumScreensRows(),
        // // settings.getNumScreensCols()));
        // }
        // } catch (FileNotFoundException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // } catch (CompilationFailedException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
        // Get the Opengl context from the drawable, and make it current, so
        // we can see it and draw on it. I've never seen this fail, but there is
        // error checking anyway.
        contextOn(drawable);

        // Once we have the context current, we can extract the OpenGL instance
        // from it. We have defined a OpenGL 3.0 instance in the
        // neonNewtWindow by adding the line
        // glp = GLProfile.get(GLProfile.GL3);
        // Therefore, we extract a GL3 instance, so we cannot make any
        // unfortunate mistakes (calls to methods that are undefined for this
        // version).
        final GL3 gl = GLContext.getCurrentGL().getGL3();

        doReshape(gl);

        contextOff(drawable);
    }

    private void doReshape(GL3 gl) {
        // set the new canvas size and aspect ratio in the global variables.
        canvasWidth = GLContext.getCurrent().getGLDrawable().getWidth();
        canvasHeight = GLContext.getCurrent().getGLDrawable().getHeight();
        aspect = (float) canvasWidth / (float) canvasHeight;

        // fontSize = (int) Math.round(w / 37.5);

        initDatastores(gl);

        reshaped = true;
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {// Get the Opengl context from
                                                  // the drawable, and make it
                                                  // current, so
        // we can see it and draw on it. I've never seen this fail, but there is
        // error checking anyway.
        contextOn(drawable);

        // Once we have the context current, we can extract the OpenGL instance
        // from it. We have defined a OpenGL 3.0 instance in the
        // neonNewtWindow by adding the line
        // glp = GLProfile.get(GLProfile.GL3);
        // Therefore, we extract a GL3 instance, so we cannot make any
        // unfortunate mistakes (calls to methods that are undefined for this
        // version).
        final GL3 gl = GLContext.getCurrentGL().getGL3();

        // Stop animation
        timer.stop();

        // Remove shaders
        try {
            loader.cleanup(gl);
        } catch (UninitializedException e) {
            e.printStackTrace();
        }

        // Close open files
        timer.close();

        // Delete models
        sphereModel.delete(gl);
        atmModel.delete(gl);
        legendModel.delete(gl);
        fsq.delete(gl);

        // Delete the FrameBuffer Objects.
        atmosphereFrameBufferObject.delete(gl);
        hudTextFrameBufferObject.delete(gl);
        legendTextureFrameBufferObject.delete(gl);
        sphereTextureFrameBufferObject.delete(gl);

        for (int i = 0; i < cachedScreens; i++) {
            cachedFrameBufferObjects[i].delete(gl);
        }

        contextOff(drawable);
    }

    public void makeSnapshot() {
        if (timer != null) {
            timer.setScreenshotNeeded(true);
        }
    }

    public BufferedImage getScreenshot() {
        BufferedImage frame = WaterCycleApp.getFrameImage();

        int frameWidth = frame.getWidth();
        int frameHeight = frame.getHeight();

        int[] frameRGB = new int[frameWidth * frameHeight];
        frame.getRGB(0, 0, frameWidth, frameHeight, frameRGB, 0, frameWidth);

        int glWidth = currentImage.getWidth();
        int glHeight = currentImage.getHeight();

        int[] glRGB = new int[glWidth * glHeight];
        currentImage.getRGB(0, 0, glWidth, glHeight, glRGB, 0, glWidth);

        Point p = WaterCycleApp.getCanvaslocation();

        for (int y = p.y; y < p.y + glHeight; y++) {
            int offset = (y - p.y) * glWidth;
            System.arraycopy(glRGB, offset, frameRGB, y * frameWidth + p.x, glWidth);
        }

        BufferedImage result = new BufferedImage(frame.getWidth(), frame.getHeight(), BufferedImage.TYPE_INT_RGB);

        result.setRGB(0, 0, result.getWidth(), result.getHeight(), frameRGB, 0, result.getWidth());

        return result;
    }

    public WaterCycleInputHandler getInputHandler() {
        return inputHandler;
    }
}
