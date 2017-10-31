package org.deeplearning4j.nn.conf.layers;

import lombok.*;
import org.deeplearning4j.nn.conf.InputPreProcessor;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.preprocessor.CnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.conf.preprocessor.RnnToFeedForwardPreProcessor;
import org.deeplearning4j.nn.params.DefaultParamInitializer;

import java.util.Arrays;

/**
 * Created by jeffreytang on 7/21/15.
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public abstract class FeedForwardLayer extends BaseLayer {
    protected int nIn;
    protected int nOut;

    public FeedForwardLayer(Builder builder) {
        super(builder);
        this.nIn = builder.nIn;
        this.nOut = builder.nOut;
    }


    @Override
    public InputType[] getOutputType(int layerIndex, @NonNull InputType... inputType) {
        if (preProcessor != null) {
            inputType = preProcessor.getOutputType(inputType);
        }
        if (inputType.length != 1 || (inputType[0].getType() != InputType.Type.FF
                        && inputType[0].getType() != InputType.Type.CNNFlat)) {
            throw new IllegalStateException("Invalid input type (layer index = " + layerIndex + ", layer name=\""
                            + getLayerName() + "\"): expected FeedForward input type. Got: " + Arrays.toString(inputType));
        }

        return new InputType[]{InputType.feedForward(nOut)};
    }

    @Override
    public void setNIn(InputType[] inputType, boolean override) {
        if (preProcessor != null) {
            inputType = preProcessor.getOutputType(inputType);
        }
        if (inputType == null || inputType.length != 1 || (inputType[0].getType() != InputType.Type.FF
                        && inputType[0].getType() != InputType.Type.CNNFlat)) {
            throw new IllegalStateException("Invalid input type (layer name=\"" + getLayerName()
                            + "\"): expected FeedForward input type. Got: "
                    + (inputType == null ? null : Arrays.toString(inputType)));
        }

        if (nIn <= 0 || override) {
            if (inputType[0].getType() == InputType.Type.FF) {
                InputType.InputTypeFeedForward f = (InputType.InputTypeFeedForward) inputType[0];
                this.nIn = f.getSize();
            } else {
                InputType.InputTypeConvolutionalFlat f = (InputType.InputTypeConvolutionalFlat) inputType[0];
                this.nIn = f.getFlattenedSize();
            }
        }
    }

    @Override
    public InputPreProcessor getPreProcessorForInputType(InputType... inputType) {
        if (inputType == null || inputType.length != 1) {
            throw new IllegalStateException("Invalid input for layer (layer name = \"" + getLayerName()
                    + "\"): input type should be length 1 (got: " + (inputType == null ? null : Arrays.toString(inputType)) + ")");
        }

        switch (inputType[0].getType()) {
            case FF:
            case CNNFlat:
                //FF -> FF and CNN (flattened format) -> FF: no preprocessor necessary
                return null;
            case RNN:
                //RNN -> FF
                return new RnnToFeedForwardPreProcessor();
            case CNN:
                //CNN -> FF
                InputType.InputTypeConvolutional c = (InputType.InputTypeConvolutional) inputType[0];
                return new CnnToFeedForwardPreProcessor(c.getHeight(), c.getWidth(), c.getDepth());
            default:
                throw new RuntimeException("Unknown input type: " + inputType);
        }
    }

    @Override
    public double getL1ByParam(String paramName) {
        switch (paramName) {
            case DefaultParamInitializer.WEIGHT_KEY:
                return l1;
            case DefaultParamInitializer.BIAS_KEY:
                return l1Bias;
            default:
                throw new IllegalStateException("Unknown parameter: \"" + paramName + "\"");
        }
    }

    @Override
    public double getL2ByParam(String paramName) {
        switch (paramName) {
            case DefaultParamInitializer.WEIGHT_KEY:
                return l2;
            case DefaultParamInitializer.BIAS_KEY:
                return l2Bias;
            default:
                throw new IllegalStateException("Unknown parameter: \"" + paramName + "\"");
        }
    }

    @Override
    public boolean isPretrainParam(String paramName) {
        return false; //No pretrain params in standard FF layers
    }

    public abstract static class Builder<T extends Builder<T>> extends BaseLayer.Builder<T> {
        protected int nIn = 0;
        protected int nOut = 0;

        /**
         * Number of inputs for the layer (usually the size of the last layer). <br>
         * Note that for Convolutional layers, this is the input depth, otherwise is the previous layer size.
         *
         * @param nIn Number of inputs for the layer
         */
        public T nIn(int nIn) {
            this.nIn = nIn;
            return (T) this;
        }

        /**
         * Number of outputs - used to set the layer size (number of units/nodes for the current layer).
         * Note that this is equivalent to {@link #units(int)}
         *
         * @param nOut Number of outputs / layer size
         */
        public T nOut(int nOut) {
            this.nOut = nOut;
            return (T) this;
        }

        /**
         * Set the number of units / layer size for this layer.<br>
         * This is equivalent to {@link #nOut(int)}
         *
         * @param units Size of the layer (number of units) / nOut
         * @see #nOut(int)
         */
        public T units(int units){
            return nOut(units);
        }
    }
}
