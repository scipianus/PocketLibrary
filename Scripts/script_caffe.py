import numpy as np
import matplotlib.pyplot as plt
import matplotlib.image as mpimg
import caffe
import glob
import os.path

#Parameters
params = {}
params['dir'] = './m_covers_0000/'
params['imageType'] = 'jpg'
params['queryImage'] = 'query.jpg'
params['idsFile'] = 'ids.txt'
params['outputFile'] = 'scores_m.txt'
params['topQuery'] = 5
params['nrImages'] = 10000
params['cnn'] = 'bvlc_alexnet'
params['cnnLayer'] = 'fc7';
params['caffe_root'] = 'C:\\Users\\scipianus\\Anaconda3\\Lib\\site-packages\\caffe'

#Load Caffe
caffe.set_mode_cpu()
net = caffe.Net('models/' + params['cnn'] + '/deploy.prototxt',
                'models/' + params['cnn'] + '/' + params['cnn'] + '.caffemodel',
                caffe.TEST)
transformer = caffe.io.Transformer({'data': net.blobs['data'].data.shape})
transformer.set_transpose('data', (2,0,1))
transformer.set_mean('data', np.load(params['caffe_root'] + '/imagenet/ilsvrc_2012_mean.npy').mean(1).mean(1))
transformer.set_raw_scale('data', 255.0)
transformer.set_channel_swap('data', (2,1,0))
net.blobs['data'].reshape(1,3,net.blobs['data'].data.shape[2],net.blobs['data'].data.shape[3])

#Load directory
filelist = glob.glob(params['dir'] + '*.' + params['imageType']) 
nrImages = min(len(filelist), params['nrImages'])
scores = np.zeros((nrImages, 4096))  
index = np.zeros(nrImages, int)

#Process directory
idsFile = open(params['idsFile'], 'r')
if os.path.isfile(params['outputFile']):
    print('Loading processed image database')
    scores = np.loadtxt(params['outputFile'], ndmin=2)
    for idx in range(nrImages):    
        line = idsFile.readline()
        index[idx] = int(line.split()[0]) - 1
else:
    print('Processing images from the database')
    cnt = 0
    line = idsFile.readline()
    for idx in range(len(filelist)):    
        if int(line.split()[0]) != idx + 1:
            continue
        line = idsFile.readline()
        print('Processing image ' + filelist[idx] + ' '+  str(cnt + 1) + '/' + str(nrImages))
        im = caffe.io.load_image(filelist[idx])  
        net.blobs['data'].data[...] = transformer.preprocess('data', im)
        net.forward()
        scores[cnt, :] = np.asarray(net.blobs[params['cnnLayer']].data)
        index[cnt] = int(idx)
        cnt = cnt + 1
        if cnt == nrImages:
            break
        
    np.savetxt(params['outputFile'], scores, fmt='%.5f');
    
#Process query image
im = caffe.io.load_image(params['queryImage'])  
net.blobs['data'].data[...] = transformer.preprocess('data', im)
out = net.forward(end='fc7')
scoresQuery = np.asarray(out['fc7'])

#Calculate similarities
scoresQueryMat = np.repeat(scoresQuery, nrImages, axis = 0)
dist = (scores - scoresQueryMat)**2
dist = dist.sum(axis=-1)
dist = np.sqrt(dist)

#Find best matches
best = dist.argsort()[:params['topQuery']]
fig = plt.figure()
subplot = fig.add_subplot(2,params['topQuery'],np.ceil(params['topQuery'] / 2.0))
subplot.set_title('Query')
image = mpimg.imread(params['queryImage'])
subplot.axis('off')
subplot.imshow(image)
for idx in range(params['topQuery']):
    subplot = fig.add_subplot(2,params['topQuery'],params['topQuery']+idx+1)
    subplot.set_title(str(index[best[idx]] + 1) + '\nDist = ' + str(dist[best[idx]]))
    image = mpimg.imread(filelist[index[best[idx]]])
    subplot.axis('off')
    subplot.imshow(image)